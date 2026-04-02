package edu.alibaba.mpc4j.common.rpc.impl.netty.simple;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 发送端Channel Handler，处理Channel生命周期事件。
 * <p>
 * 由于客户端只负责发送数据、不接收响应，所以channelRead方法为空。
 * </p>
 * <p>
 * <code>@ChannelHandler.Sharable</code> 注解说明：
 * <ul>
 *   <li>表示此Handler可以安全地被多个Channel共享</li>
 *   <li>要求Handler内部不持有任何Channel相关的状态（如remoteAddress）</li>
 *   <li>本类无实例变量依赖，因此可以安全共享</li>
 * </ul>
 * </p>
 *
 * @author Li Peng
 * @date 2020/10/12
 */
@ChannelHandler.Sharable
public class SimpleDataSendHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDataSendHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 当Pipeline中任何Handler抛出异常时，异常会沿Pipeline传播，最终调用此方法
        // 对于SimpleNettyRpc，我们假设网络稳定，异常应快速暴露而非静默恢复
        // 在发生异常时，记录错误并关闭Channel
        LOGGER.error("Exception caught in send handler, closing channel: {}", ctx.channel(), cause);
        ctx.close();
    }
}