package edu.alibaba.mpc4j.common.rpc.impl.netty;

import com.google.protobuf.ByteString;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.NettyRpcProtobuf.DataPacketProto;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.NettyRpcProtobuf.DataPacketProto.HeaderProto;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.NettyRpcProtobuf.DataPacketProto.PayloadProto;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.NettyRpcProtobuf.DataPacketProto.TypeProto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketBuffer;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.rpc.utils.PayloadType;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ServerHandler for handling received data.
 *
 * @author Feng Qing, Weiran Liu
 * @date 2020/10/12
 */
@ChannelHandler.Sharable
public class DataReceiveHandler extends ChannelInboundHandlerAdapter {
    /**
     * buffer
     */
    private final DataPacketBuffer dataPacketBuffer;

    DataReceiveHandler(DataPacketBuffer dataPacketBuffer) {
        this.dataPacketBuffer = dataPacketBuffer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // read data packet from channel
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
        List<byte[]> payload;
        switch (payloadType) {
            case NORMAL:
            case EMPTY:
            case SINGLETON:
                payload = payloadProto.getPayloadBytesList().stream()
                .map(ByteString::toByteArray)
                .collect(Collectors.toList());
                break;
            case EQUAL_SIZE:
                int length = IntUtils.byteArrayToInt(payloadProto.getPayloadBytes(0).toByteArray());
                payload = SerializeUtils.decompressEqual(payloadProto.getPayloadBytes(1).toByteArray(), length);
                break;
            default:
                throw new IllegalStateException("Invalid " + PayloadType.class.getSimpleName() + ": " + payloadType);
        }
        // put data into the buffer
        dataPacketBuffer.put(DataPacket.fromByteArrayList(header, payload));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        // 什么也不做
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 捕获到任何异常时都会调用exceptionCaught， 打印异常栈跟踪
        cause.printStackTrace();
        ctx.close();
    }
}
