package edu.alibaba.mpc4j.common.rpc.impl.netty;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.NettyRpcProtobuf;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import java.net.InetSocketAddress;

/**
 * 数据发送方管理器，只负责发送数据，使用channelPool来维持一个连接池
 *
 * @author Li Peng, Weiran Liu
 * @date 2020/10/12
 */
public class DataSendManager {
    /**
     * ClientHandler
     */
    private final DataSendHandler dataSendHandler;
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
     */
    public DataSendManager() {
        dataSendHandler = new DataSendHandler();
        senderBootstrap = new Bootstrap();
        // 非阻塞模式
        senderBootstrap.group(new NioEventLoopGroup()).channel(NioSocketChannel.class);
        // 设置channelPool
        poolMap = new AbstractChannelPoolMap<InetSocketAddress, FixedChannelPool>() {
            @Override
            protected FixedChannelPool newPool(InetSocketAddress key) {
                ChannelPoolHandler handler = new ChannelPoolHandler() {

                    @Override
                    public void channelReleased(Channel ch) {

                    }

                    @Override
                    public void channelCreated(Channel channel) {
                        // 创建连接时添加clientHandler，只有当channel不足时会创建，但不会超过限制的最大channel数
                        SocketChannel ch = (SocketChannel)channel;
                        ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                        ch.pipeline().addLast(new ProtobufEncoder());
                        ch.pipeline().addLast(dataSendHandler);
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
    public void sendData(NettyParty receiver, NettyRpcProtobuf.DataPacketProto dataPacketProto) {
        // 首先获取receiver主机对应的channelPool
        Preconditions.checkNotNull(dataPacketProto);
        // poolMap.get永远会返回一个pool。如果key对应的pool还不存在，那会新建一个pool并返回
        SimpleChannelPool simpleChannelPool = this.poolMap.get(
            new InetSocketAddress(receiver.getHost(), receiver.getPort())
        );
        // 从连接池中尝试获取一个channel
        Future<Channel> f = simpleChannelPool.acquire();
        f.addListener((FutureListener<Channel>)futureChannel -> {
            if (futureChannel.isSuccess()) {
                Channel ch = futureChannel.getNow();
                ch.writeAndFlush(dataPacketProto);
                simpleChannelPool.release(ch);
            }
        });
    }
}
