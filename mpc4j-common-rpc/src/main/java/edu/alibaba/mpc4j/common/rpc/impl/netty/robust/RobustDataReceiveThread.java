package edu.alibaba.mpc4j.common.rpc.impl.netty.robust;

import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyParty;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.RobustNettyRpcProtobuf;
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
 * <p>
 * 与SimpleDataReceiveThread的区别：
 * <ul>
 *   <li>使用RobustDataReceiveHandler替代SimpleDataReceiveHandler</li>
 *   <li>ProtobufDecoder解码RobustMessageProto（包含ChunkProto的oneof字段）</li>
 * </ul>
 * </p>
 *
 * @author Weiran Liu
 * @date 2026/04/02
 */
public class RobustDataReceiveThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(RobustDataReceiveThread.class);
    /**
     * 自己的参与方信息
     */
    private final NettyParty ownParty;
    /**
     * CyclicBarrier，用于与主线程同步（close()完成后通知主线程）
     */
    private final CyclicBarrier cyclicBarrier;
    /**
     * 数据缓冲区
     */
    private final DataPacketBuffer dataPacketBuffer;
    /**
     * 分片重组器
     */
    private final RobustChunkAssembler robustChunkAssembler;
    /**
     * BossGroup：处理Accept事件
     */
    private EventLoopGroup bossGroup;
    /**
     * WorkerGroup：处理Read/Write事件
     */
    private EventLoopGroup workerGroup;
    /**
     * 服务端ServerChannel
     */
    private Channel channel;

    /**
     * 构建数据接收管理器。
     *
     * @param ownParty             本参与方信息（含监听端口）
     * @param cyclicBarrier        用于close()完成后与主线程同步
     * @param dataPacketBuffer     完整DataPacket的接收缓冲区
     * @param robustChunkAssembler 分片重组器
     */
    RobustDataReceiveThread(
        NettyParty ownParty,
        CyclicBarrier cyclicBarrier,
        DataPacketBuffer dataPacketBuffer,
        RobustChunkAssembler robustChunkAssembler
    ) {
        this.ownParty = ownParty;
        this.cyclicBarrier = cyclicBarrier;
        this.dataPacketBuffer = dataPacketBuffer;
        this.robustChunkAssembler = robustChunkAssembler;
        bossGroup = null;
        workerGroup = null;
        channel = null;
    }

    @Override
    public void run() {
        try {
            RobustDataReceiveHandler robustDataReceiveHandler = new RobustDataReceiveHandler(
                dataPacketBuffer, robustChunkAssembler
            );
            // (1) 创建EventLoopGroup
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            // (2) 创建ServerBootstrap
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                // (3) 指定NIO传输Channel
                .channel(NioServerSocketChannel.class)
                // (4) 绑定端口
                .localAddress(new InetSocketAddress(ownParty.getPort()))
                // (5) 为每个新建立的子Channel配置pipeline
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        // 数据流向：网络字节流 → FrameDecoder → ProtobufDecoder → RobustDataReceiveHandler
                        // ProtobufVarint32FrameDecoder: 解决粘包/拆包问题
                        ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                        // 解码为RobustMessageProto（包含ChunkProto或AckProto的oneof）
                        // 帧大小限制由ProtobufVarint32FrameDecoder负责（默认无限制）
                        ch.pipeline().addLast(new ProtobufDecoder(
                            RobustNettyRpcProtobuf.RobustMessageProto.getDefaultInstance()
                        ));
                        // 业务Handler：处理分片重组和ACK回送
                        ch.pipeline().addLast(robustDataReceiveHandler);
                    }
                });
            // (6) 异步绑定，sync()阻塞等待绑定完成
            ChannelFuture f = b.bind().sync();
            // (7) 阻塞直到ServerChannel关闭
            channel = f.channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            LOGGER.error("Receive thread interrupted unexpectedly", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 关闭服务端，释放所有资源。
     * <p>
     * 关闭流程：
     * <ol>
     *   <li>优雅关闭BossGroup（停止接收新连接）</li>
     *   <li>优雅关闭WorkerGroup（处理完已接收的请求后关闭），完成后通知CyclicBarrier</li>
     *   <li>关闭ServerChannel</li>
     * </ol>
     * </p>
     *
     * @throws InterruptedException 若等待过程中线程被中断
     */
    public void close() throws InterruptedException {
        this.bossGroup.shutdownGracefully().sync();
        this.workerGroup.shutdownGracefully().addListener(future -> {
            // WorkerGroup关闭完成后，通过CyclicBarrier通知主线程
            cyclicBarrier.await();
        });
        channel.close();
    }
}
