package edu.alibaba.mpc4j.common.rpc.impl.netty.robust;

import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.RobustNettyRpcProtobuf.ChunkProto;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.RobustNettyRpcProtobuf.ChunkProto.HeaderProto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.rpc.utils.PayloadType;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据分片序列化/反序列化与重组器，线程安全。
 * <p>
 * 负责三件事：（1）将DataPacket的payload序列化为字节数组（{@link #serialize}）；
 * （2）将多个ChunkProto重组为完整的序列化字节数组；
 * （3）将字节数组反序列化为DataPacket（{@link #deserialize}）。
 * 序列化与反序列化逻辑集中于此，发送端{@link RobustNettyRpc}通过静态方法调用。
 * </p>
 * <h2>内存开销</h2>
 * <p>
 * 发送方在每个ChunkProto的 {@code totalBytes} 字段中携带完整payload的精确字节数，
 * 接收方在收到首片时一次性预分配大小为 {@code totalBytes} 的 {@code byte[]}。
 * 每收到一片，立即将其内容写入 {@code output} 数组的对应偏移位置后释放，
 * 全程只持有这一份输出缓冲区，无需额外拷贝，峰值内存约 1N。
 * </p>
 * <h2>幂等性</h2>
 * <p>
 * 发送方writeAndFlush失败后会重传同一分片，接收端可能收到重复chunk。
 * {@code AssembleState.written} 数组记录每个chunkIndex是否已写入，
 * 重复到达的分片会被直接跳过，确保输出结果不受重传影响。
 * </p>
 *
 * @author Weiran Liu
 * @date 2026/04/02
 */
class RobustChunkAssembler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RobustChunkAssembler.class);
    /**
     * 分片大小：1MB
     */
    static final int CHUNK_SIZE = 1024 * 1024;

    /**
     * 待重组DataPacket的中间状态。
     * <ul>
     *   <li>{@code output}：按 {@code totalBytes} 预分配的输出缓冲区，分片按偏移直接写入</li>
     *   <li>{@code written}：每个chunkIndex是否已写入，用于幂等保护（忽略重传的重复分片）</li>
     *   <li>{@code received}：已成功写入的不重复分片数，全部到齐时等于 {@code totalChunks}</li>
     *   <li>{@code totalChunks}：该DataPacket的总分片数</li>
     * </ul>
     */
    private static class AssembleState {
        /**
         * 预分配的输出缓冲区，大小精确等于发送方序列化payload的字节数
         */
        final byte[] output;
        /**
         * 每个chunkIndex是否已写入：true=已写入，false=尚未收到；用于忽略重传的重复分片
         */
        final boolean[] written;
        /**
         * 该DataPacket的总分片数
         */
        final int totalChunks;
        /**
         * 已成功写入 output 的不重复分片数量
         */
        int received;

        AssembleState(int totalBytes, int totalChunks) {
            this.output = new byte[totalBytes];
            this.written = new boolean[totalChunks];
            this.totalChunks = totalChunks;
            this.received = 0;
        }
    }

    /**
     * 待重组的分片存储。
     * key为DataPacket的唯一标识HeaderProto（包含encodeTaskId/ptoId/stepId/extraInfo/senderId/receiverId），
     * value为对应的重组状态。
     * <p>
     * HeaderProto继承自protobuf的AbstractMessage，已正确实现hashCode()和equals()，
     * 且hashCode有memoizedHashCode缓存，适合直接作为Map的key。
     * </p>
     */
    private final ConcurrentHashMap<HeaderProto, AssembleState> pendingStates;

    RobustChunkAssembler() {
        pendingStates = new ConcurrentHashMap<>();
    }

    /**
     * 将DataPacket的payload序列化为字节数组。
     * <p>
     * 序列化格式：
     * <ul>
     *   <li>NORMAL/EMPTY/SINGLETON: {@code [count(4B)][len0(4B)][data0][len1(4B)][data1]...}</li>
     *   <li>EQUAL_SIZE: {@code [length(4B)][compressedData]}</li>
     * </ul>
     * </p>
     *
     * @param payloadType payload类型
     * @param payload     payload数据
     * @return 序列化后的字节数组
     */
    static byte[] serialize(PayloadType payloadType, List<byte[]> payload) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            switch (payloadType) {
                case NORMAL, EMPTY, SINGLETON -> {
                    baos.write(IntUtils.intToByteArray(payload.size()));
                    for (byte[] data : payload) {
                        baos.write(IntUtils.intToByteArray(data.length));
                        baos.write(data);
                    }
                }
                case EQUAL_SIZE -> {
                    int length = payload.isEmpty() ? 0 : payload.get(0).length;
                    byte[] compressed = SerializeUtils.compressEqual(payload, length);
                    baos.write(IntUtils.intToByteArray(length));
                    baos.write(compressed);
                }
                default -> throw new IllegalStateException(
                    "Invalid " + PayloadType.class.getSimpleName() + ": " + payloadType);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
    }

    /**
     * 添加一个分片；若该DataPacket的所有分片均已到达，则重组并反序列化为DataPacket返回。
     * <p>
     * 分片到达时立即将其内容直接写入预分配缓冲区的对应偏移处（零额外拷贝）。
     * 全部分片写入后，对字节数组执行反序列化并返回完整DataPacket。
     * </p>
     *
     * @param chunk 收到的分片
     * @return 若所有分片已收齐，返回反序列化后的DataPacket；否则返回null
     */
    DataPacket addChunk(ChunkProto chunk) {
        HeaderProto header = chunk.getHeaderProto();
        int totalChunks = chunk.getTotalChunks();
        int totalBytes = (int) chunk.getTotalBytes();
        int chunkIndex = chunk.getChunkIndex();

        // 为该DataPacket初始化重组状态（若尚未初始化）
        // computeIfAbsent保证原子性，多个线程同时到达时只初始化一次
        AssembleState state = pendingStates.computeIfAbsent(
            header, k -> new AssembleState(totalBytes, totalChunks)
        );

        // 计算当前分片在输出缓冲区中的写入偏移量
        // chunkIndex * CHUNK_SIZE 对应分片在完整payload中的起始字节位置
        byte[] data = chunk.getChunkData().toByteArray();
        int offset = chunkIndex * CHUNK_SIZE;
        // 幂等保护：若该chunkIndex已写入（重传片），直接跳过，避免重复计数
        if (state.written[chunkIndex]) {
            LOGGER.debug("Duplicate chunk ignored: index={}, header={}", chunkIndex, header);
            return null;
        }
        // 将分片内容直接写入预分配缓冲区，写入后 data 可被GC
        System.arraycopy(data, 0, state.output, offset, data.length);
        state.written[chunkIndex] = true;
        state.received++;

        if (state.received < state.totalChunks) {
            // 还有分片未到达，等待
            return null;
        }
        // 所有分片已写入，从pendingStates中移除，防止内存泄漏
        pendingStates.remove(header);
        LOGGER.debug("All {} chunks assembled for header: {}", totalChunks, header);
        return deserialize(chunk, state.output);
    }

    /**
     * 将重组后的字节数组反序列化为DataPacket。
     * <p>
     * 序列化格式由{@link #serialize}定义：
     * <ul>
     *   <li>NORMAL/EMPTY/SINGLETON: {@code [count(4B)][len0(4B)][data0][len1(4B)][data1]...}</li>
     *   <li>EQUAL_SIZE: {@code [length(4B)][compressedData]}</li>
     * </ul>
     * </p>
     *
     * @param chunk    分片（提供header和typeProto信息）
     * @param fullData 重组后的完整字节数组
     * @return 反序列化后的DataPacket；反序列化失败时返回null
     */
    private static DataPacket deserialize(ChunkProto chunk, byte[] fullData) {
        HeaderProto headerProto = chunk.getHeaderProto();
        DataPacketHeader header = new DataPacketHeader(
            headerProto.getEncodeTaskId(),
            headerProto.getPtoId(),
            headerProto.getStepId(),
            headerProto.getExtraInfo(),
            headerProto.getSenderId(),
            headerProto.getReceiverId()
        );
        PayloadType payloadType = PayloadType.values()[chunk.getTypeProto().getTypeId()];
        List<byte[]> payload;
        try {
            payload = switch (payloadType) {
                case NORMAL, EMPTY, SINGLETON -> deserializeNormal(fullData);
                case EQUAL_SIZE -> deserializeEqualSize(fullData);
            };
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize DataPacket for header: {}", header, e);
            return null;
        }
        return DataPacket.fromByteArrayList(header, payload);
    }

    /**
     * 反序列化NORMAL/EMPTY/SINGLETON类型的payload。
     * 格式：[count(4B)][len0(4B)][data0][len1(4B)][data1]...
     */
    private static List<byte[]> deserializeNormal(byte[] fullData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(fullData);
        int count = IntUtils.byteArrayToInt(bais.readNBytes(Integer.BYTES));
        List<byte[]> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int len = IntUtils.byteArrayToInt(bais.readNBytes(Integer.BYTES));
            result.add(bais.readNBytes(len));
        }
        return result;
    }

    /**
     * 反序列化EQUAL_SIZE类型的payload。
     * 格式：[length(4B)][compressedData]
     */
    private static List<byte[]> deserializeEqualSize(byte[] fullData) {
        int length = IntUtils.byteArrayToInt(Arrays.copyOfRange(fullData, 0, Integer.BYTES));
        byte[] compressed = Arrays.copyOfRange(fullData, Integer.BYTES, fullData.length);
        return SerializeUtils.decompressEqual(compressed, length);
    }
}
