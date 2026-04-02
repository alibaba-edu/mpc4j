package edu.alibaba.mpc4j.common.rpc.impl.netty.robust;

import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.RobustNettyRpcProtobuf.ChunkProto;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.RobustNettyRpcProtobuf.RobustMessageProto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketBuffer;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 接收端Channel Handler，处理入站数据分片。
 * <p>
 * Pipeline数据流：
 * 网络字节流 → ProtobufVarint32FrameDecoder → ProtobufDecoder → 本Handler.channelRead()
 * </p>
 * <p>
 * 处理逻辑：
 * <ul>
 *   <li>收到ChunkProto后交给RobustChunkAssembler做幂等重组；</li>
 *   <li>所有分片到齐时，将反序列化得到的DataPacket放入DataPacketBuffer供业务层receive()取用。</li>
 * </ul>
 * </p>
 * <p>
 * 与SimpleDataReceiveHandler的区别：
 * <ul>
 *   <li>exceptionCaught只关闭出问题的子Channel，不影响ServerChannel继续监听，
 *       对端重连后可自动恢复接收</li>
 *   <li>增加了分片重组逻辑；重传片通过幂等写入自动忽略</li>
 * </ul>
 * </p>
 * <p>
 * {@code @ChannelHandler.Sharable}注解说明：
 * <ul>
 *   <li>dataPacketBuffer、robustChunkAssembler均为线程安全实现</li>
 * </ul>
 * </p>
 *
 * @author Weiran Liu
 * @date 2026/04/02
 */
@ChannelHandler.Sharable
class RobustDataReceiveHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RobustDataReceiveHandler.class);
    /**
     * 数据缓冲区，用于存放重组完成的完整DataPacket
     */
    private final DataPacketBuffer dataPacketBuffer;
    /**
     * 分片重组器，负责收集分片、幂等重组字节数组并反序列化为DataPacket，线程安全
     */
    private final RobustChunkAssembler robustChunkAssembler;

    RobustDataReceiveHandler(
        DataPacketBuffer dataPacketBuffer,
        RobustChunkAssembler robustChunkAssembler
    ) {
        this.dataPacketBuffer = dataPacketBuffer;
        this.robustChunkAssembler = robustChunkAssembler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // Pipeline中的ProtobufDecoder已将msg解码为RobustMessageProto对象
        RobustMessageProto message = (RobustMessageProto) msg;
        ChunkProto chunk = message.getChunk();
        if (chunk != ChunkProto.getDefaultInstance()) {
            handleChunk(chunk);
        } else {
            LOGGER.warn("Received message with no chunk: {}", message);
        }
    }

    /**
     * 处理收到的数据分片：交给重组器；若所有分片已到齐，将DataPacket放入buffer。
     *
     * @param chunk 收到的分片
     */
    private void handleChunk(ChunkProto chunk) {
        // 将分片交给重组器（幂等：重传的重复分片会被忽略）
        DataPacket dataPacket = robustChunkAssembler.addChunk(chunk);
        // 若所有分片均已到达，将DataPacket放入buffer供业务层receive()取用
        if (dataPacket != null) {
            dataPacketBuffer.put(dataPacket);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        // 无需特殊处理
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 与SimpleDataReceiveHandler的区别：
        // 只关闭出问题的子Channel（单个TCP连接），不影响ServerChannel继续监听新连接。
        // 发送方重连后，新的子Channel会被ServerChannel accept，接收可自动恢复。
        LOGGER.warn("Exception in receive channel, closing this connection: {}", ctx.channel(), cause);
        ctx.close();
    }
}
