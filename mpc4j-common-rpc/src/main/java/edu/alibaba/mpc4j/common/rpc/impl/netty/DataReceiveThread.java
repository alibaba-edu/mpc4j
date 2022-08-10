package edu.alibaba.mpc4j.common.rpc.impl.netty;

import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.NettyRpcProtobuf;
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

import java.net.InetSocketAddress;
import java.util.concurrent.CyclicBarrier;

/**
 * 数据接收方管理器，只负责接收数据。
 *
 * @author Li Peng, Weiran Liu
 * @date 2020/10/12
 */
public class DataReceiveThread extends Thread {
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
    public DataReceiveThread(NettyParty ownParty, CyclicBarrier cyclicBarrier, DataPacketBuffer dataPacketBuffer) {
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
            DataReceiveHandler dataReceiveHandler = new DataReceiveHandler(dataPacketBuffer);
            // (1) 创建EventLoopGroup
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
                        // 由于使用protobuf作为协议解析，需要先添加以下两个Decoder()
                        ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                        ch.pipeline().addLast(
                            new ProtobufDecoder(NettyRpcProtobuf.DataPacketProto.getDefaultInstance())
                        );
                        // 自定义的协议解析handler
                        ch.pipeline().addLast(dataReceiveHandler);
                    }
                });
            // (6) 异步地绑定服务器；调用 sync()方法阻塞等待直到绑定完成
            ChannelFuture f = b.bind().sync();
            // (7) 获取 Channel 的CloseFuture，并且阻塞当前线程直到它完成
            channel = f.channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭server端的group，也会关闭server channel
     */
    public void close() throws InterruptedException {
        this.bossGroup.shutdownGracefully().sync();
        this.workerGroup.shutdownGracefully().addListener(future -> {
            // 添加一个GenericFutureListener()来监听操作完成
            cyclicBarrier.await();
        });
        channel.close();
    }
}
