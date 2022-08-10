package edu.alibaba.mpc4j.common.rpc.impl.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 接收数据handler，由于我们的client只负责发不负责收，所以方法空置即可
 *
 * @author Li Peng
 * @date 2020/10/12
 */
@ChannelHandler.Sharable
public class DataSendHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 在发生异常时，记录错误并关闭Channel
        cause.printStackTrace();
        ctx.close();
    }
}