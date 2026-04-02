package edu.alibaba.mpc4j.common.rpc.impl.netty.robust;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyParty;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.RobustNettyRpcProtobuf.ChunkProto;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.RobustNettyRpcProtobuf.RobustMessageProto;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 数据发送管理器，支持分片发送和写失败时指数退避重传。
 * <p>
 * 核心机制：
 * <ol>
 *   <li>分片发送：调用方将序列化后的字节数组按CHUNK_SIZE拆分为ChunkProto，本类负责逐片发送</li>
 *   <li>写失败重传：当acquire或writeAndFlush失败时，通过EventLoop.schedule()异步延迟重试，
 *       不阻塞Netty IO线程；最多重试MAX_RETRY次，总等待约3.1秒</li>
 *   <li>接收端幂等：重传时接收端可能已收过该chunk，RobustChunkAssembler会忽略重复分片</li>
 * </ol>
 * </p>
 *
 * @author Weiran Liu
 * @date 2026/04/02
 */
public class RobustDataSendManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RobustDataSendManager.class);
    /**
     * 最大重试次数（5次，退避时间：100+200+400+800+1600=3100ms）
     */
    private static final int MAX_RETRY = 5;
    /**
     * 初始退避时间（毫秒）
     */
    private static final long INITIAL_BACKOFF_MS = 100;
    /**
     * 发送端Handler（可共享）
     */
    private final RobustDataSendHandler robustDataSendHandler;
    /**
     * 客户端Bootstrap
     */
    private final Bootstrap senderBootstrap;
    /**
     * 连接池映射，每个远程地址对应一个FixedChannelPool
     */
    private final ChannelPoolMap<InetSocketAddress, FixedChannelPool> poolMap;

    /**
     * 构建发送管理器（不带故障注入Handler）。
     */
    RobustDataSendManager() {
        this(null);
    }

    /**
     * 构建发送管理器。
     * <p>
     * 初始化流程：
     * <ol>
     *   <li>创建可共享的RobustDataSendHandler</li>
     *   <li>创建Bootstrap并配置EventLoopGroup和Channel类型</li>
     *   <li>创建ChannelPoolMap，按远程地址管理连接池</li>
     * </ol>
     * </p>
     *
     * @param extraHandler 可选的额外pipeline Handler（测试用，如故障注入Handler）；传null则不注入
     */
    RobustDataSendManager(ChannelHandler extraHandler) {
        robustDataSendHandler = new RobustDataSendHandler();
        senderBootstrap = new Bootstrap();
        senderBootstrap.group(new NioEventLoopGroup()).channel(NioSocketChannel.class);
        poolMap = new AbstractChannelPoolMap<>() {
            @Override
            protected FixedChannelPool newPool(InetSocketAddress key) {
                ChannelPoolHandler handler = new ChannelPoolHandler() {
                    @Override
                    public void channelReleased(Channel ch) {
                        // 归还Channel时无需特殊处理
                    }

                    @Override
                    public void channelCreated(Channel channel) {
                        // 新建Channel时配置pipeline
                        SocketChannel ch = (SocketChannel) channel;
                        // 在消息前添加varint32格式的长度字段，用于接收端分帧
                        ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                        // 将Protobuf消息对象编码为字节数组
                        ch.pipeline().addLast(new ProtobufEncoder());
                        // 可选的额外Handler（如测试用的故障注入Handler），插在编码器之后、业务Handler之前
                        if (extraHandler != null) {
                            ch.pipeline().addLast(extraHandler);
                        }
                        // 处理Channel生命周期事件（异常时WARN+关闭，不抛出）
                        ch.pipeline().addLast(robustDataSendHandler);
                    }

                    @Override
                    public void channelAcquired(Channel ch) {
                        // 获取Channel时无需特殊处理
                    }
                };
                return new FixedChannelPool(senderBootstrap.remoteAddress(key), handler, 20);
            }
        };
    }

    /**
     * 发送一个ChunkProto，等待"写入网络成功"后返回（不等对端确认）。
     * <p>
     * 若writeAndFlush失败，按指数退避重试最多MAX_RETRY次。
     * 超过重试次数则抛出RuntimeException。
     * 接收端通过幂等写入保证重传不会造成数据错误。
     * </p>
     *
     * @param receiver 接收方
     * @param chunk    要发送的分片
     */
    void sendChunk(NettyParty receiver, ChunkProto chunk) {
        Preconditions.checkNotNull(receiver);
        Preconditions.checkNotNull(chunk);
        // 将ChunkProto包装为顶层消息RobustMessageProto
        RobustMessageProto message = RobustMessageProto.newBuilder().setChunk(chunk).build();
        // 使用CompletableFuture等待"写入成功"信号（只等写入，不等对端确认）
        CompletableFuture<Void> writeFuture = new CompletableFuture<>();
        sendMessageWithRetry(receiver, message, 0, INITIAL_BACKOFF_MS, writeFuture);
        try {
            writeFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while sending chunk to " + receiver, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to send chunk to " + receiver, e.getCause());
        }
    }

    /**
     * 发送消息，失败时指数退避重试。
     * <p>
     * 重试通过EventLoop.schedule()实现异步延迟，不阻塞Netty IO线程。
     * writeAndFlush成功后complete writeFuture，最终重试失败后completeExceptionally。
     * </p>
     *
     * @param receiver    接收方
     * @param message     待发送的顶层消息
     * @param attempt     当前重试次数（从0开始）
     * @param backoffMs   当前退避时间（毫秒）
     * @param writeFuture 用于通知调用方"写入完成"的Future
     */
    private void sendMessageWithRetry(
        NettyParty receiver, RobustMessageProto message, int attempt, long backoffMs,
        CompletableFuture<Void> writeFuture
    ) {
        FixedChannelPool pool = poolMap.get(new InetSocketAddress(receiver.getHost(), receiver.getPort()));
        // acquire()是异步操作，通过FutureListener回调处理结果
        Future<Channel> acquireFuture = pool.acquire();
        acquireFuture.addListener((FutureListener<Channel>) futureChannel -> {
            if (futureChannel.isSuccess()) {
                Channel ch = futureChannel.getNow();
                // writeAndFlush也是异步操作，将消息写入发送缓冲区后立即返回
                ChannelFuture wf = ch.writeAndFlush(message);
                wf.addListener(writeResult -> {
                    pool.release(ch);
                    if (writeResult.isSuccess()) {
                        // 写入网络成功，通知调用方
                        writeFuture.complete(null);
                    } else {
                        LOGGER.warn("writeAndFlush failed to {} (attempt {}/{})", receiver, attempt + 1, MAX_RETRY, writeResult.cause());
                        retryOrFail(receiver, message, attempt, backoffMs, writeFuture, ch);
                    }
                });
            } else {
                LOGGER.warn("acquire channel failed to {} (attempt {}/{})", receiver, attempt + 1, MAX_RETRY, futureChannel.cause());
                // acquire失败时没有Channel可用，借用NioEventLoopGroup的任意一个线程来调度重试
                senderBootstrap.config().group().schedule(
                    () -> retryOrFail(receiver, message, attempt, backoffMs, writeFuture, null),
                    backoffMs, TimeUnit.MILLISECONDS
                );
            }
        });
    }

    /**
     * 检查重试次数，未超限则通过EventLoop.schedule()异步延迟重试，超限则completeExceptionally。
     *
     * @param receiver    接收方
     * @param message     待发送的消息
     * @param attempt     当前已失败次数
     * @param backoffMs   当前退避时间
     * @param writeFuture 用于通知调用方结果的Future
     * @param ch          出错的Channel（有则用其EventLoop调度，否则用group线程）
     */
    private void retryOrFail(
        NettyParty receiver, RobustMessageProto message, int attempt, long backoffMs,
        CompletableFuture<Void> writeFuture, Channel ch
    ) {
        if (attempt < MAX_RETRY) {
            Runnable retryTask = () -> sendMessageWithRetry(receiver, message, attempt + 1, backoffMs * 2, writeFuture);
            if (ch != null) {
                ch.eventLoop().schedule(retryTask, backoffMs, TimeUnit.MILLISECONDS);
            } else {
                senderBootstrap.config().group().schedule(retryTask, backoffMs, TimeUnit.MILLISECONDS);
            }
        } else {
            // 超过最大重试次数，通过Future传递失败
            LOGGER.error("Send failed after {} retries to {}", MAX_RETRY, receiver);
            writeFuture.completeExceptionally(
                new RuntimeException("Send failed after " + MAX_RETRY + " retries to " + receiver)
            );
        }
    }

    /**
     * 关闭发送管理器，释放所有资源。
     * <p>
     * 关闭流程：
     * <ol>
     *   <li>关闭所有连接池中的Channel</li>
     *   <li>关闭EventLoopGroup（NIO线程池）并等待完成</li>
     * </ol>
     * </p>
     */
    void close() {
        if (poolMap instanceof AbstractChannelPoolMap<InetSocketAddress, FixedChannelPool> abstractPoolMap) {
            for (java.util.Map.Entry<InetSocketAddress, FixedChannelPool> entry : abstractPoolMap) {
                FixedChannelPool pool = entry.getValue();
                if (pool != null) {
                    pool.close();
                }
            }
        }
        if (senderBootstrap.config().group() != null) {
            try {
                senderBootstrap.config().group().shutdownGracefully().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
