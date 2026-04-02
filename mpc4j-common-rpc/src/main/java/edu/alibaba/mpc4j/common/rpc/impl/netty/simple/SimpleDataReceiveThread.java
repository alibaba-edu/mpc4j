package edu.alibaba.mpc4j.common.rpc.impl.netty.simple;

import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyParty;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.SimpleNettyRpcProtobuf;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketBuffer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CyclicBarrier;

/**
 * 数据接收方管理器，只负责接收数据。
 * <p>
 * Netty服务端线程模型：
 * <ul>
 *   <li>BossGroup: 负责接收客户端连接（Accept事件），每个服务端只需一个线程</li>
 *   <li>WorkerGroup: 负责处理已建立连接的读写事件（Read/Write）</li>
 *   <li>ChannelPipeline: 入站数据依次流经各Handler（解码器 → 业务Handler）</li>
 * </ul>
 * </p>
 *
 * @author Li Peng, Weiran Liu
 * @date 2020/10/12
 */
public class SimpleDataReceiveThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDataReceiveThread.class);
    /**
     * 自己的参与方信息
     */
    private final NettyParty ownParty;
    /**
     * CyclicBarrier，用于多线程同步
     */
    private final CyclicBarrier cyclicBarrier;
    /**
     * 数据缓冲区
     */
    private final DataPacketBuffer dataPacketBuffer;
    /**
     * BossGroup用来处理nio的Accept
     */
    private EventLoopGroup bossGroup;
    /**
     * WorkerGroup处理nio的Read和Write事件
     */
    private EventLoopGroup workerGroup;
    /**
     * 服务端channel
     */
    private Channel channel;

    /**
     * 构建数据接收管理器
     *
     * @param ownParty 参与方自身信息
     * @param cyclicBarrier 用于线程同步的cyclicBarrier
     */
    public SimpleDataReceiveThread(NettyParty ownParty, CyclicBarrier cyclicBarrier, DataPacketBuffer dataPacketBuffer) {
        this.ownParty = ownParty;
        this.dataPacketBuffer = dataPacketBuffer;
        this.cyclicBarrier = cyclicBarrier;
        bossGroup = null;
        workerGroup = null;
        channel = null;
    }

    @Override
    public void run() {
        try {
            SimpleDataReceiveHandler simpleDataReceiveHandler = new SimpleDataReceiveHandler(dataPacketBuffer);
            // (1) 创建EventLoopGroup
            // BossGroup处理Accept，WorkerGroup处理Read/Write，都是NIO线程池
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            // (2) 创建ServerBootstrap
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                // (3) 指定所使用的 NIO 传输 Channel
                .channel(NioServerSocketChannel.class)
                // (4) 使用指定的端口设置套接字地址
                .localAddress(new InetSocketAddress(ownParty.getPort()))
                // (5) 添加Handler
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        // 为每个新建立的连接配置pipeline（入站数据的处理链）
                        // 数据流向：网络字节流 → FrameDecoder → ProtobufDecoder → 自定义Handler
                        // ProtobufVarint32FrameDecoder: 根据varint32长度字段分割TCP流，解决粘包/拆包问题
                        ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                        // ProtobufDecoder: 将字节数组解码为Protobuf消息对象
                        ch.pipeline().addLast(
                            new ProtobufDecoder(SimpleNettyRpcProtobuf.DataPacketProto.getDefaultInstance())
                        );
                        // 自定义Handler: 处理解码后的业务数据
                        ch.pipeline().addLast(simpleDataReceiveHandler);
                    }
                });
            // (6) 异步地绑定服务器；调用 sync()方法阻塞等待直到绑定完成
            // bind()返回ChannelFuture（异步），sync()阻塞当前线程直到绑定完成
            ChannelFuture f = b.bind().sync();
            // (7) 获取 Channel 的CloseFuture，并且阻塞当前线程直到它完成
            // closeFuture()返回一个Future，当Channel关闭时该Future完成
            // sync()阻塞当前线程，使run()方法持续运行直到Channel关闭
            channel = f.channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            LOGGER.error("Receive thread interrupted unexpectedly", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 关闭server端的group，也会关闭server channel
     * <p>
     * 关闭流程：
     * <ol>
     *   <li>优雅关闭BossGroup（停止接收新连接）</li>
     *   <li>优雅关闭WorkerGroup（处理完已接收的请求后关闭）</li>
     *   <li>关闭完成后通知CyclicBarrier，让主线程继续</li>
     *   <li>关闭ServerChannel</li>
     * </ol>
     * </p>
     */
    public void close() throws InterruptedException {
        // shutdownGracefully: 优雅关闭，不再接受新任务，但等待已提交任务完成
        this.bossGroup.shutdownGracefully().sync();
        this.workerGroup.shutdownGracefully().addListener(future -> {
            // 添加一个GenericFutureListener()来监听操作完成
            // WorkerGroup关闭完成后，通过CyclicBarrier通知主线程
            cyclicBarrier.await();
        });
        channel.close();
    }
}
