package edu.alibaba.mpc4j.common.rpc.impl.netty.robust;

import com.google.protobuf.ByteString;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.RobustNettyRpcProtobuf.ChunkProto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.rpc.utils.PayloadType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * RobustChunkAssembler单元测试。
 * <p>
 * 测试场景：
 * <ol>
 *   <li>单片DataPacket（空payload，totalChunks=1）正常重组</li>
 *   <li>多片DataPacket按顺序到达，正常重组</li>
 *   <li>多片DataPacket乱序到达，仍能正确重组</li>
 *   <li>重复chunk（模拟重传）被幂等跳过，结果不受影响</li>
 *   <li>不同DataPacket（不同HeaderProto）并发重组互不干扰</li>
 * </ol>
 * </p>
 *
 * @author Weiran Liu
 * @date 2026/04/02
 */
public class RobustChunkAssemblerTest {
    /**
     * protocol ID
     */
    private static final int PTO_ID = 1;
    /**
     * step ID
     */
    private static final int STEP_ID = 0;
    /**
     * sender ID
     */
    private static final int SENDER_ID = 0;
    /**
     * receiver ID
     */
    private static final int RECEIVER_ID = 1;

    private static List<ChunkProto> makeChunks(long encodeTaskId, byte[] serialized) {
        int chunkSize = RobustChunkAssembler.CHUNK_SIZE;
        int totalChunks = Math.max(1, (serialized.length + chunkSize - 1) / chunkSize);
        ChunkProto.HeaderProto headerProto = ChunkProto.HeaderProto.newBuilder()
            .setEncodeTaskId(encodeTaskId)
            .setPtoId(PTO_ID)
            .setStepId(STEP_ID)
            .setExtraInfo(0)
            .setSenderId(SENDER_ID)
            .setReceiverId(RECEIVER_ID)
            .build();
        ChunkProto.TypeProto typeProto = ChunkProto.TypeProto.newBuilder()
            .setTypeId(PayloadType.NORMAL.ordinal())
            .build();
        ChunkProto[] chunks = new ChunkProto[totalChunks];
        for (int i = 0; i < totalChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, serialized.length);
            chunks[i] = ChunkProto.newBuilder()
                .setHeaderProto(headerProto)
                .setTypeProto(typeProto)
                .setChunkIndex(i)
                .setTotalChunks(totalChunks)
                .setTotalBytes(serialized.length)
                .setChunkData(ByteString.copyFrom(Arrays.copyOfRange(serialized, start, end)))
                .build();
        }
        return Arrays.asList(chunks);
    }

    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public RobustChunkAssemblerTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testEmptyChunk() {
        RobustChunkAssembler assembler = new RobustChunkAssembler();
        byte[] serialized = RobustChunkAssembler.serialize(PayloadType.EMPTY, Collections.emptyList());
        List<ChunkProto> chunks = makeChunks(1L, serialized);
        Assert.assertEquals(1, chunks.size());

        DataPacket result = assembler.addChunk(chunks.get(0));
        Assert.assertNotNull("Single chunk should complete immediately", result);
        Assert.assertEquals(0, result.getPayload().size());
    }

    @Test
    public void testSingleChunk() {
        RobustChunkAssembler assembler = new RobustChunkAssembler();
        byte[] data = new byte[1024];
        secureRandom.nextBytes(data);
        byte[] serialized = RobustChunkAssembler.serialize(PayloadType.SINGLETON, Collections.singletonList(data));
        List<ChunkProto> chunks = makeChunks(2L, serialized);
        Assert.assertEquals(1, chunks.size());

        DataPacket result = assembler.addChunk(chunks.get(0));
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getPayload().size());
        Assert.assertArrayEquals(data, result.getPayload().get(0));
    }

    @Test
    public void testMultiChunkInOrder() {
        RobustChunkAssembler assembler = new RobustChunkAssembler();
        // 构造 2.5MB 数据
        int dataSize = (int) (2.5 * 1024 * 1024);
        int expectedChunks = CommonUtils.getUnitNum(dataSize, RobustChunkAssembler.CHUNK_SIZE);
        byte[] data = new byte[dataSize];
        secureRandom.nextBytes(data);
        byte[] serialized = RobustChunkAssembler.serialize(PayloadType.NORMAL, Collections.singletonList(data));
        List<ChunkProto> chunks = makeChunks(3L, serialized);
        Assert.assertEquals(expectedChunks, chunks.size());

        // 前两片返回 null
        Assert.assertNull("Chunk 0 should return null", assembler.addChunk(chunks.get(0)));
        Assert.assertNull("Chunk 1 should return null", assembler.addChunk(chunks.get(1)));
        // 最后一片返回完整 DataPacket
        DataPacket result = assembler.addChunk(chunks.get(2));
        Assert.assertNotNull("Last chunk should return DataPacket", result);
        Assert.assertEquals(1, result.getPayload().size());
        Assert.assertArrayEquals(data, result.getPayload().get(0));
    }

    /**
     * 多片DataPacket乱序到达（2,0,1），仍能正确重组。
     */
    @Test
    public void testMultiChunkOutOfOrder() {
        RobustChunkAssembler assembler = new RobustChunkAssembler();
        int dataSize = (int) (2.5 * 1024 * 1024);
        int expectedChunks = CommonUtils.getUnitNum(dataSize, RobustChunkAssembler.CHUNK_SIZE);
        byte[] data = new byte[dataSize];
        secureRandom.nextBytes(data);
        byte[] serialized = RobustChunkAssembler.serialize(PayloadType.NORMAL, Collections.singletonList(data));
        List<ChunkProto> chunks = makeChunks(4L, serialized);
        Assert.assertEquals(expectedChunks, chunks.size());

        // 乱序到达：2 → 0 → 1
        Assert.assertNull(assembler.addChunk(chunks.get(2)));
        Assert.assertNull(assembler.addChunk(chunks.get(0)));
        DataPacket result = assembler.addChunk(chunks.get(1));
        Assert.assertNotNull("Last arriving chunk should complete reassembly", result);
        Assert.assertArrayEquals(data, result.getPayload().get(0));
    }

    /**
     * 重复chunk（模拟TCP闪断后重传）被幂等跳过，不影响最终结果。
     * <p>
     * 场景：发送方在发送第0片后channel断连，重传第0片；
     * 接收端第一次写入后，重传的第0片应被直接忽略，received不重复计数。
     * </p>
     */
    @Test
    public void testDuplicateChunkIdempotent() {
        RobustChunkAssembler assembler = new RobustChunkAssembler();
        int dataSize = (int) (2.5 * 1024 * 1024);
        int expectedChunks = CommonUtils.getUnitNum(dataSize, RobustChunkAssembler.CHUNK_SIZE);
        byte[] data = new byte[dataSize];
        secureRandom.nextBytes(data);
        byte[] serialized = RobustChunkAssembler.serialize(PayloadType.NORMAL, Collections.singletonList(data));
        List<ChunkProto> chunks = makeChunks(5L, serialized);
        Assert.assertEquals(expectedChunks, chunks.size());

        // chunk 0 正常到达
        Assert.assertNull(assembler.addChunk(chunks.get(0)));
        // chunk 0 重传（幂等，应返回 null，received 不重复计数）
        Assert.assertNull("Duplicate chunk 0 should be ignored (idempotent)", assembler.addChunk(chunks.get(0)));
        // chunk 0 再次重传
        Assert.assertNull("Duplicate chunk 0 (2nd retransmit) should be ignored", assembler.addChunk(chunks.get(0)));
        // chunk 1 到达
        Assert.assertNull(assembler.addChunk(chunks.get(1)));
        // chunk 2 到达，此时 received=3，应完成重组
        DataPacket result = assembler.addChunk(chunks.get(2));
        Assert.assertNotNull("All distinct chunks received, should complete reassembly", result);
        Assert.assertArrayEquals("Reassembled data must match original", data, result.getPayload().get(0));
    }

    /**
     * 两个不同DataPacket（不同encodeTaskId）并发加入，互不干扰。
     */
    @Test
    public void testTwoDataPacketsIndependent() {
        RobustChunkAssembler assembler = new RobustChunkAssembler();
        int dataSize = (int) (1.5 * 1024 * 1024);
        byte[] data1 = new byte[dataSize];
        int expectedData1Chunks = CommonUtils.getUnitNum(dataSize, RobustChunkAssembler.CHUNK_SIZE);
        byte[] data2 = new byte[dataSize];
        int expectedData2Chunks = CommonUtils.getUnitNum(dataSize, RobustChunkAssembler.CHUNK_SIZE);
        secureRandom.nextBytes(data1);
        secureRandom.nextBytes(data2);

        byte[] serialized1 = RobustChunkAssembler.serialize(PayloadType.NORMAL, Collections.singletonList(data1));
        byte[] serialized2 = RobustChunkAssembler.serialize(PayloadType.NORMAL, Collections.singletonList(data2));
        // 两个DataPacket用不同 encodeTaskId 区分（模拟不同协议步骤）
        List<ChunkProto> chunks1 = makeChunks(10L, serialized1);
        List<ChunkProto> chunks2 = makeChunks(20L, serialized2);
        Assert.assertEquals(expectedData1Chunks, chunks1.size());
        Assert.assertEquals(expectedData2Chunks, chunks2.size());

        // 交错加入：pkt1-chunk0, pkt2-chunk0, pkt1-chunk1, pkt2-chunk1
        Assert.assertNull(assembler.addChunk(chunks1.get(0)));
        Assert.assertNull(assembler.addChunk(chunks2.get(0)));
        DataPacket result1 = assembler.addChunk(chunks1.get(1));
        DataPacket result2 = assembler.addChunk(chunks2.get(1));

        Assert.assertNotNull("DataPacket1 should complete after its last chunk", result1);
        Assert.assertNotNull("DataPacket2 should complete after its last chunk", result2);

        // 验证各自内容正确，不串台
        DataPacketHeader header1 = result1.getHeader();
        DataPacketHeader header2 = result2.getHeader();
        Assert.assertEquals(10L, header1.getEncodeTaskId());
        Assert.assertEquals(20L, header2.getEncodeTaskId());
        Assert.assertArrayEquals(data1, result1.getPayload().get(0));
        Assert.assertArrayEquals(data2, result2.getPayload().get(0));
    }
}
