package edu.alibaba.mpc4j.common.rpc.impl.netty;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.SimpleNettyRpcProtobuf;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * 数据发送方管理器，只负责发送数据，使用channelPool来维持一个连接池。
 * <p>
 * Netty连接池的核心概念：
 * <ul>
 *   <li>Bootstrap: Netty客户端启动器，用于配置并生成Channel</li>
 *   <li>FixedChannelPool: 固定大小的连接池，每个远程地址对应一个池，池内最多20个Channel</li>
 *   <li>Channel: 代表一个TCP连接，可复用以发送多个消息</li>
 *   <li>ChannelPipeline: Channel内的处理链，由多个Handler组成，数据依次流经各Handler</li>
 * </ul>
 * </p>
 *
 * @author Li Peng, Weiran Liu
 * @date 2020/10/12
 */
public class SimpleDataSendManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDataSendManager.class);
    /**
     * ClientHandler
     */
    private final SimpleDataSendHandler simpleDataSendHandler;
    /**
     * 引导
     */
    private final Bootstrap senderBootstrap;
    /**
     * 用于管理不同连接池的map，其中每个key对应一个远程地址
     */
    public ChannelPoolMap<InetSocketAddress, FixedChannelPool> poolMap;

    /**
     * 构建client。
     * <p>
     * 初始化流程：
     * <ol>
     *   <li>创建Handler实例（共享，用于所有Channel）</li>
     *   <li>创建Bootstrap并配置EventLoopGroup（线程池）和Channel类型</li>
     *   <li>创建ChannelPoolMap，按远程地址(InetSocketAddress)管理多个连接池</li>
     * </ol>
     * </p>
     */
    public SimpleDataSendManager() {
        simpleDataSendHandler = new SimpleDataSendHandler();
        senderBootstrap = new Bootstrap();
        // NioEventLoopGroup: Netty的NIO线程组，处理所有Channel的IO事件
        // NioSocketChannel: 使用NIO的客户端TCP Channel
        senderBootstrap.group(new NioEventLoopGroup()).channel(NioSocketChannel.class);
        // 设置channelPool
        poolMap = new AbstractChannelPoolMap<>() {
            @Override
            protected FixedChannelPool newPool(InetSocketAddress key) {
                ChannelPoolHandler handler = new ChannelPoolHandler() {

                    @Override
                    public void channelReleased(Channel ch) {

                    }

                    @Override
                    public void channelCreated(Channel channel) {
                        // 当连接池需要新建Channel时调用此方法，配置Channel的pipeline
                        // Pipeline是处理链，数据出站时依次流经各Handler（编码器）
                        SocketChannel ch = (SocketChannel) channel;
                        // ProtobufVarint32LengthFieldPrepender: 在消息前添加varint32格式的长度字段
                        ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                        // ProtobufEncoder: 将Protobuf消息对象编码为字节数组
                        ch.pipeline().addLast(new ProtobufEncoder());
                        // 自定义Handler: 处理Channel生命周期事件（如异常）
                        ch.pipeline().addLast(simpleDataSendHandler);
                    }

                    @Override
                    public void channelAcquired(Channel ch) {

                    }
                };
                // 单个host连接池大小，maxConnections暂时设置成20，设置得过小（如3）运行时会出错。
                return new FixedChannelPool(senderBootstrap.remoteAddress(key), handler, 20);
            }
        };
    }

    /**
     * 发送数据。
     *
     * @param receiver        接收方。
     * @param dataPacketProto 用protobuf封装的数据包。
     */
    public void sendData(NettyParty receiver, SimpleNettyRpcProtobuf.DataPacketProto dataPacketProto) {
        // 首先获取receiver主机对应的channelPool
        Preconditions.checkNotNull(dataPacketProto);
        // poolMap.get永远会返回一个pool。如果key对应的pool还不存在，那会新建一个pool并返回
        SimpleChannelPool simpleChannelPool = this.poolMap.get(
            new InetSocketAddress(receiver.getHost(), receiver.getPort())
        );
        // 从连接池中尝试获取一个channel
        // acquire()是异步操作：提交获取请求后立即返回Future，实际结果通过回调获取
        // 这种模式避免了阻塞调用线程，让IO操作在EventLoop线程中完成
        Future<Channel> f = simpleChannelPool.acquire();
        // 添加监听器：当acquire完成时（无论成功或失败）在IO线程中被回调
        f.addListener((FutureListener<Channel>) futureChannel -> {
            if (futureChannel.isSuccess()) {
                // if acquire is successful, get channel, send data and get future
                Channel ch = futureChannel.getNow();
                // writeAndFlush也是异步：将数据写入发送缓冲区后立即返回ChannelFuture
                // 实际发送和对端接收完成由回调通知
                ChannelFuture writeFuture = ch.writeAndFlush(dataPacketProto);
                // 监听write完成事件，确保数据已写入OS缓冲区
                writeFuture.addListener(wf -> {
                    // release channel after write when write is complete
                    simpleChannelPool.release(ch);
                    // if write is not successful, throw an exception
                    if (!wf.isSuccess()) {
                        LOGGER.error("Failed to send data packet to {}", receiver, wf.cause());
                        throw new RuntimeException("writeAndFlush failed to " + receiver, wf.cause());
                    }
                });
            } else {
                // if acquire is not successful, throw an exception
                LOGGER.error("Failed to acquire channel to {}", receiver, futureChannel.cause());
                throw new RuntimeException("acquire channel failed to " + receiver, futureChannel.cause());
            }
        });
    }
}
