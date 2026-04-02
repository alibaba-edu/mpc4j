package edu.alibaba.mpc4j.common.rpc.impl.netty.robust;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 发送端Channel Handler，处理Channel生命周期事件。
 * <p>
 * 与SimpleDataSendHandler的区别：
 * <ul>
 *   <li>exceptionCaught只记录WARN日志并关闭Channel，不抛出异常</li>
 *   <li>关闭坏的Channel后，FixedChannelPool会将其标记为无效，下次acquire时自动创建新Channel</li>
 *   <li>实际的重试逻辑由RobustDataSendManager负责</li>
 * </ul>
 * </p>
 * <p>
 * {@code @ChannelHandler.Sharable}注解说明：
 * <ul>
 *   <li>表示此Handler可以安全地被多个Channel共享</li>
 *   <li>本类无实例变量依赖，因此可以安全共享</li>
 * </ul>
 * </p>
 *
 * @author Weiran Liu
 * @date 2026/04/02
 */
@ChannelHandler.Sharable
public class RobustDataSendHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RobustDataSendHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 连接建立，无需特殊处理
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 发送端不处理入站消息
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 与SimpleDataSendHandler不同：不抛出异常，只记录WARN并关闭Channel。
        // 关闭后，FixedChannelPool不会复用此Channel，下次acquire时会建立新连接。
        // 重试逻辑由RobustDataSendManager.sendChunkWithRetry()负责。
        LOGGER.warn("Exception in send channel, closing for reconnect: {}", ctx.channel(), cause);
        ctx.close();
    }
}
