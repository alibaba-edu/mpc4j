package edu.alibaba.mpc4j.common.rpc.impl.netty;

import com.google.protobuf.ByteString;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.SimpleNettyRpcProtobuf.DataPacketProto;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.SimpleNettyRpcProtobuf.DataPacketProto.HeaderProto;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.SimpleNettyRpcProtobuf.DataPacketProto.PayloadProto;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.SimpleNettyRpcProtobuf.DataPacketProto.TypeProto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketBuffer;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.rpc.utils.PayloadType;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 接收端Channel Handler，处理入站数据。
 * <p>
 * 当Channel收到数据时，Netty会依次调用pipeline中的Handler：
 * ProtobufVarint32FrameDecoder → ProtobufDecoder → 本Handler.channelRead()
 * </p>
 * <p>
 * <code>@ChannelHandler.Sharable</code>注解说明：
 * <ul>
 *   <li>表示此Handler可以安全地被多个Channel共享</li>
 *   <li>dataPacketBuffer是线程安全的，可以被多Channel并发写入</li>
 * </ul>
 * </p>
 *
 * @author Feng Qing, Weiran Liu
 * @date 2020/10/12
 */
@ChannelHandler.Sharable
public class SimpleDataReceiveHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDataReceiveHandler.class);
    /**
     * buffer
     */
    private final DataPacketBuffer dataPacketBuffer;

    SimpleDataReceiveHandler(DataPacketBuffer dataPacketBuffer) {
        this.dataPacketBuffer = dataPacketBuffer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 当Pipeline中的上一个Handler（ProtobufDecoder）完成解码后，调用此方法
        // ProtobufDecoder已经把msg解码为DataPacketProto对象
        DataPacketProto dataPacketProto = (DataPacketProto) msg;
        // handle header
        HeaderProto headerProto = dataPacketProto.getHeaderProto();
        long encodeTaskId = headerProto.getEncodeTaskId();
        int ptoId = headerProto.getPtoId();
        int stepId = headerProto.getStepId();
        long extraInfo = headerProto.getExtraInfo();
        int senderId = headerProto.getSenderId();
        int receiverId = headerProto.getReceiverId();
        DataPacketHeader header = new DataPacketHeader(encodeTaskId, ptoId, stepId, extraInfo, senderId, receiverId);
        // handle type
        TypeProto typeProto = dataPacketProto.getTypeProto();
        PayloadType payloadType = PayloadType.values()[typeProto.getTypeId()];
        // handle payload
        PayloadProto payloadProto = dataPacketProto.getPayloadProto();
        List<byte[]> payload = switch (payloadType) {
            case NORMAL, EMPTY, SINGLETON -> payloadProto.getPayloadBytesList().stream()
                .map(ByteString::toByteArray)
                .collect(Collectors.toList());
            case EQUAL_SIZE -> {
                int length = IntUtils.byteArrayToInt(payloadProto.getPayloadBytes(0).toByteArray());
                yield SerializeUtils.decompressEqual(payloadProto.getPayloadBytes(1).toByteArray(), length);
            }
        };
        // put data into the buffer
        dataPacketBuffer.put(DataPacket.fromByteArrayList(header, payload));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        // 什么也不做
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 当Pipeline中任何Handler抛出异常时，异常会沿Pipeline传播，最终调用此方法
        // 对于SimpleNettyRpc，我们假设网络稳定，因此应该快速暴露异常：调用exceptionCaught，记录异常并关闭channel
        LOGGER.error("Exception caught in receive handler, closing channel: {}", ctx.channel(), cause);
        ctx.close();
    }
}
