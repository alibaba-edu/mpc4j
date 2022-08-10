package edu.alibaba.mpc4j.common.rpc.impl.netty;

import com.google.protobuf.ByteString;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.NettyRpcProtobuf;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketBuffer;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ServerHandler，用于处理接收数据。
 *
 * @author Feng Qing, Weiran Liu
 * @date 2020/10/12
 */
@ChannelHandler.Sharable
public class DataReceiveHandler extends ChannelInboundHandlerAdapter {
    /**
     * 缓冲区
     */
    private final DataPacketBuffer dataPacketBuffer;

    DataReceiveHandler(DataPacketBuffer dataPacketBuffer) {
        this.dataPacketBuffer = dataPacketBuffer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 读取channel中发过来的数据，并给予protobuf规则进行解析
        NettyRpcProtobuf.DataPacketProto dataPacketProto = (NettyRpcProtobuf.DataPacketProto)msg;
        // 恢复数据包head
        NettyRpcProtobuf.DataPacketProto.HeaderProto headerProto = dataPacketProto.getHeaderProto();
        long taskId = headerProto.getTaskId();
        int ptoId = headerProto.getPtoId();
        int stepId = headerProto.getStepId();
        long extraInfo = headerProto.getExtraInfo();
        int senderId = headerProto.getSenderId();
        int receiverId = headerProto.getReceiverId();
        DataPacketHeader header = new DataPacketHeader(taskId, ptoId, stepId, extraInfo, senderId, receiverId);
        // 恢复数据包payload
        NettyRpcProtobuf.DataPacketProto.PayloadProto payloadProto = dataPacketProto.getPayloadProto();
        List<byte[]> payload = payloadProto.getPayloadBytesList().stream()
            .map(ByteString::toByteArray)
            .collect(Collectors.toList());
        // 放入数据缓存区
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
