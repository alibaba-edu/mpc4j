package edu.alibaba.mpc4j.common.rpc.impl.netty.robust;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyParty;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.RobustNettyRpcProtobuf.ChunkProto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketBuffer;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.rpc.utils.PayloadType;
import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

/**
 * 健壮版Netty RPC，支持大包分片+写失败重传（应对TCP闪断）。
 * <p>
 * 与SimpleNettyRpc相比，核心区别：
 * <ul>
 *   <li>send()将DataPacket序列化后按1MB分片逐片发送；writeAndFlush失败时指数退避重传；对上层业务完全透明。</li>
 *   <li>接收端对重传的重复分片做幂等处理（已写入的chunkIndex直接跳过）。</li>
 *   <li>接收端ServerChannel在子Channel异常时保持存活，对端重连后自动恢复。</li>
 * </ul>
 * </p>
 *
 * @author Weiran Liu
 * @date 2026/04/02
 */
public class RobustNettyRpc implements Rpc {
    private static final Logger LOGGER = LoggerFactory.getLogger(RobustNettyRpc.class);
    /**
     * 参与方ID映射
     */
    private final HashMap<Integer, NettyParty> partyIdHashMap;
    /**
     * 自己的参与方信息
     */
    private final NettyParty ownParty;
    /**
     * Own party's ID
     */
    private final int ownPartyId;
    /**
     * 数据接收缓存区
     */
    private final DataPacketBuffer dataPacketBuffer;
    /**
     * CyclicBarrier，用于多线程同步。每次connect()时创建新实例。
     */
    private CyclicBarrier cyclicBarrier;
    /**
     * 数据接收线程
     */
    private RobustDataReceiveThread robustDataReceiveThread;
    /**
     * 数据发送管理器
     */
    private RobustDataSendManager robustDataSendManager;
    /**
     * 分片重组器
     */
    private final RobustChunkAssembler robustChunkAssembler;
    /**
     * 连接状态：true表示已连接，false表示已断开
     */
    private boolean connected;
    /**
     * 数据包数量
     */
    private long dataPacketNum;
    /**
     * 负载字节长度
     */
    private long payloadByteLength;
    /**
     * 发送字节长度
     */
    private long sendByteLength;
    /**
     * 可选的发送管道额外Handler（如测试用的故障注入Handler）；null表示不注入
     */
    private final ChannelHandler extraSendHandler;

    /**
     * 构建RobustNettyRpc。
     *
     * @param ownParty         参与方信息。
     * @param partySet         所有参与方集合。
     * @param extraSendHandler 可选的发送管道额外Handler（如故障注入Handler）；传null则不注入。
     */
    public RobustNettyRpc(NettyParty ownParty, Set<NettyParty> partySet, ChannelHandler extraSendHandler) {
        Preconditions.checkArgument(partySet.size() > 1, "Party set size must be greater than 1");
        Preconditions.checkArgument(partySet.contains(ownParty), "Party set must contain own party");
        testPortInUse(ownParty.getPort());
        this.ownParty = ownParty;
        ownPartyId = ownParty.getPartyId();
        partyIdHashMap = new HashMap<>();
        partySet.forEach(party -> partyIdHashMap.put(party.getPartyId(), party));
        dataPacketNum = 0;
        payloadByteLength = 0;
        sendByteLength = 0;
        connected = false;
        cyclicBarrier = null;
        robustDataReceiveThread = null;
        robustDataSendManager = null;
        robustChunkAssembler = new RobustChunkAssembler();
        dataPacketBuffer = new DataPacketBuffer();
        this.extraSendHandler = extraSendHandler;
    }

    /**
     * 检测端口是否已被占用，若占用则直接退出。
     *
     * @param port 端口号
     */
    private void testPortInUse(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
        } catch (IOException e) {
            LOGGER.error("configure error, port: {} may be already in use. please check the running process or change the port", port, e);
            System.exit(1);
        }
    }

    @Override
    public Party ownParty() {
        return ownParty;
    }

    @Override
    public Set<Party> getPartySet() {
        return partyIdHashMap.keySet().stream().map(partyIdHashMap::get).collect(Collectors.toSet());
    }

    @Override
    public Party getParty(int partyId) {
        assert (partyIdHashMap.containsKey(partyId));
        return partyIdHashMap.get(partyId);
    }

    @Override
    public void connect() {
        if (connected) {
            LOGGER.warn("{} already connected, skip connect()", ownParty);
            return;
        }
        cyclicBarrier = new CyclicBarrier(2);
        // 先开启数据发送服务
        robustDataSendManager = new RobustDataSendManager(extraSendHandler);
        // 再开启数据接收服务
        robustDataReceiveThread = new RobustDataReceiveThread(
            ownParty, cyclicBarrier, dataPacketBuffer, robustChunkAssembler
        );
        robustDataReceiveThread.start();
        // 握手：保持无限等待语义（网络假定可连通）
        partyIdHashMap.keySet().stream().sorted().forEach(otherPartyId -> {
            if (otherPartyId < ownPartyId) {
                DataPacketHeader clientConnectHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, RobustNettyPtoDesc.getInstance().getPtoId(),
                    RobustNettyPtoDesc.StepEnum.CLIENT_CONNECT.ordinal(), ownPartyId, otherPartyId
                );
                DataPacketHeader serverConnectHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, RobustNettyPtoDesc.getInstance().getPtoId(),
                    RobustNettyPtoDesc.StepEnum.SERVER_CONNECT.ordinal(), otherPartyId, ownPartyId
                );
                DataPacketHeader clientConfirmHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, RobustNettyPtoDesc.getInstance().getPtoId(),
                    RobustNettyPtoDesc.StepEnum.CLIENT_CONFIRM.ordinal(), ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(clientConnectHeader, new LinkedList<>()));
                while (receiveWithSleep(serverConnectHeader) == null) {
                    LOGGER.info("{} requests connection with {}",
                        partyIdHashMap.get(ownPartyId), partyIdHashMap.get(otherPartyId));
                    send(DataPacket.fromByteArrayList(clientConnectHeader, new LinkedList<>()));
                }
                send(DataPacket.fromByteArrayList(clientConfirmHeader, new LinkedList<>()));
                dataPacketBuffer.clearBuffer(serverConnectHeader);
                LOGGER.info("{} successfully make connection with {}",
                    partyIdHashMap.get(ownPartyId), partyIdHashMap.get(otherPartyId));
            } else if (otherPartyId > ownPartyId) {
                DataPacketHeader clientConnectHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, RobustNettyPtoDesc.getInstance().getPtoId(),
                    RobustNettyPtoDesc.StepEnum.CLIENT_CONNECT.ordinal(), otherPartyId, ownPartyId
                );
                DataPacketHeader serverConnectHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, RobustNettyPtoDesc.getInstance().getPtoId(),
                    RobustNettyPtoDesc.StepEnum.SERVER_CONNECT.ordinal(), ownPartyId, otherPartyId
                );
                DataPacketHeader clientConfirmHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, RobustNettyPtoDesc.getInstance().getPtoId(),
                    RobustNettyPtoDesc.StepEnum.CLIENT_CONFIRM.ordinal(), otherPartyId, ownPartyId
                );
                while (receiveWithSleep(clientConnectHeader) == null) {
                    LOGGER.info("{} requests being connected with {}",
                        partyIdHashMap.get(ownPartyId), partyIdHashMap.get(otherPartyId));
                }
                send(DataPacket.fromByteArrayList(serverConnectHeader, new LinkedList<>()));
                while (receiveWithSleep(clientConfirmHeader) == null) {
                    LOGGER.info("{} requests confirm from {}",
                        partyIdHashMap.get(ownPartyId), partyIdHashMap.get(otherPartyId));
                    send(DataPacket.fromByteArrayList(serverConnectHeader, new LinkedList<>()));
                }
                dataPacketBuffer.clearBuffer(clientConnectHeader);
                dataPacketBuffer.clearBuffer(clientConfirmHeader);
                LOGGER.info("{} successfully make connection with {}",
                    partyIdHashMap.get(ownPartyId), partyIdHashMap.get(otherPartyId));
            }
        });
        connected = true;
        LOGGER.info("{} connected", ownParty);
    }

    /**
     * Sleep for a while and then try to immediately receive data packet that matches the header from buffer.
     *
     * @param header header.
     * @return data packet that matches the header; null if there is no such data packet.
     */
    private DataPacket receiveWithSleep(DataPacketHeader header) {
        Preconditions.checkArgument(ownPartyId == header.getReceiverId(), "Receiver ID must be %s", ownPartyId);
        Preconditions.checkArgument(partyIdHashMap.containsKey(header.getSenderId()),
            "Party set does not contain Sender ID = %s", header.getSenderId());
        try {
            Thread.sleep(100);
            return dataPacketBuffer.takeImmediately(header);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public void send(DataPacket dataPacket) {
        DataPacketHeader header = dataPacket.getHeader();
        Preconditions.checkArgument(ownPartyId == header.getSenderId(), "Sender ID must be %s", ownPartyId);
        Preconditions.checkArgument(partyIdHashMap.containsKey(header.getReceiverId()),
            "Party set does not contain Receiver ID = %s", header.getReceiverId());
        // 统计payload字节数（序列化前的原始大小）
        payloadByteLength += dataPacket.getPayload().stream().mapToLong(d -> d.length).sum();
        dataPacketNum++;
        // 序列化 + 分片发送（等待每片写入网络成功）
        sendInChunks(dataPacket);
    }

    /**
     * 将DataPacket序列化后按CHUNK_SIZE分片，逐片发送，等待每片写入网络成功后再发下一片。
     * <p>
     * 若writeAndFlush失败，自动指数退避重传（最多5次）。接收端通过幂等处理忽略重复分片。
     * </p>
     * <p>
     * 序列化格式：
     * <ul>
     *   <li>NORMAL/EMPTY/SINGLETON: [count(4B)][len0(4B)][data0][len1(4B)][data1]...</li>
     *   <li>EQUAL_SIZE: [length(4B)][compressedData]</li>
     * </ul>
     * </p>
     *
     * @param dataPacket 待发送的DataPacket
     */
    private void sendInChunks(DataPacket dataPacket) {
        DataPacketHeader header = dataPacket.getHeader();
        PayloadType payloadType = dataPacket.getPayloadType();
        List<byte[]> payload = dataPacket.getPayload();
        // 序列化 payload 为字节数组（格式由 RobustChunkAssembler.serialize 统一定义）
        byte[] serializedPayload = RobustChunkAssembler.serialize(payloadType, payload);
        sendByteLength += serializedPayload.length;
        int totalChunks = Math.max(1, (serializedPayload.length + RobustChunkAssembler.CHUNK_SIZE - 1) / RobustChunkAssembler.CHUNK_SIZE);
        NettyParty receiver = partyIdHashMap.get(header.getReceiverId());
        ChunkProto.HeaderProto headerProto = ChunkProto.HeaderProto.newBuilder()
            .setEncodeTaskId(header.getEncodeTaskId())
            .setPtoId(header.getPtoId())
            .setStepId(header.getStepId())
            .setExtraInfo(header.getExtraInfo())
            .setSenderId(header.getSenderId())
            .setReceiverId(header.getReceiverId())
            .build();
        ChunkProto.TypeProto typeProto = ChunkProto.TypeProto.newBuilder()
            .setTypeId(payloadType.ordinal())
            .build();
        for (int i = 0; i < totalChunks; i++) {
            int start = i * RobustChunkAssembler.CHUNK_SIZE;
            int end = Math.min(start + RobustChunkAssembler.CHUNK_SIZE, serializedPayload.length);
            byte[] chunkData = Arrays.copyOfRange(serializedPayload, start, end);
            ChunkProto chunk = ChunkProto.newBuilder()
                .setHeaderProto(headerProto)
                .setTypeProto(typeProto)
                .setChunkIndex(i)
                .setTotalChunks(totalChunks)
                .setTotalBytes(serializedPayload.length)
                .setChunkData(ByteString.copyFrom(chunkData))
                .build();
            // sendChunk等待写入网络成功后返回（失败时自动重传）
            robustDataSendManager.sendChunk(receiver, chunk);
        }
    }

    @Override
    public DataPacket receive(DataPacketHeader header) {
        Preconditions.checkArgument(ownPartyId == header.getReceiverId(), "Receiver ID must be %s", ownPartyId);
        Preconditions.checkArgument(partyIdHashMap.containsKey(header.getSenderId()),
            "Party set does not contain Sender ID = %s", header.getSenderId());
        try {
            return dataPacketBuffer.take(header);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public DataPacket receiveAny(int ptoId) {
        try {
            return dataPacketBuffer.take(ownPartyId, ptoId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public long getPayloadByteLength() {
        return payloadByteLength;
    }

    @Override
    public long getSendByteLength() {
        return sendByteLength;
    }

    @Override
    public long getSendDataPacketNum() {
        return dataPacketNum;
    }

    @Override
    public void reset() {
        payloadByteLength = 0;
        sendByteLength = 0;
        dataPacketNum = 0;
    }

    @Override
    public void synchronize() {
        partyIdHashMap.keySet().stream().sorted().forEach(otherPartyId -> {
            if (otherPartyId < ownPartyId) {
                DataPacketHeader clientSynchronizeHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, RobustNettyPtoDesc.getInstance().getPtoId(),
                    RobustNettyPtoDesc.StepEnum.CLIENT_SYNCHRONIZE.ordinal(), ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(clientSynchronizeHeader, new LinkedList<>()));
                DataPacketHeader serverSynchronizeHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, RobustNettyPtoDesc.getInstance().getPtoId(),
                    RobustNettyPtoDesc.StepEnum.SERVER_SYNCHRONIZE.ordinal(), otherPartyId, ownPartyId
                );
                receive(serverSynchronizeHeader);
            } else if (otherPartyId > ownPartyId) {
                DataPacketHeader clientSynchronizeHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, RobustNettyPtoDesc.getInstance().getPtoId(),
                    RobustNettyPtoDesc.StepEnum.CLIENT_SYNCHRONIZE.ordinal(), otherPartyId, ownPartyId
                );
                receive(clientSynchronizeHeader);
                DataPacketHeader serverSynchronizeHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, RobustNettyPtoDesc.getInstance().getPtoId(),
                    RobustNettyPtoDesc.StepEnum.SERVER_SYNCHRONIZE.ordinal(), ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(serverSynchronizeHeader, new LinkedList<>()));
            }
        });
        LOGGER.info("{} synchronized", ownParty);
    }

    @Override
    public void disconnect() {
        if (!connected) {
            LOGGER.warn("{} already disconnected, skip disconnect()", ownParty);
            return;
        }
        partyIdHashMap.keySet().stream().sorted().forEach(otherPartyId -> {
            if (otherPartyId < ownPartyId) {
                DataPacketHeader clientFinishHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, RobustNettyPtoDesc.getInstance().getPtoId(),
                    RobustNettyPtoDesc.StepEnum.CLIENT_FINISH.ordinal(), ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(clientFinishHeader, new LinkedList<>()));
                DataPacketHeader serverFinishHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, RobustNettyPtoDesc.getInstance().getPtoId(),
                    RobustNettyPtoDesc.StepEnum.SERVER_FINISH.ordinal(), otherPartyId, ownPartyId
                );
                receive(serverFinishHeader);
            } else if (otherPartyId > ownPartyId) {
                DataPacketHeader clientFinishHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, RobustNettyPtoDesc.getInstance().getPtoId(),
                    RobustNettyPtoDesc.StepEnum.CLIENT_FINISH.ordinal(), otherPartyId, ownPartyId
                );
                receive(clientFinishHeader);
                DataPacketHeader serverFinishHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, RobustNettyPtoDesc.getInstance().getPtoId(),
                    RobustNettyPtoDesc.StepEnum.SERVER_FINISH.ordinal(), ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(serverFinishHeader, new LinkedList<>()));
            }
        });
        try {
            robustDataReceiveThread.close();
            cyclicBarrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            LOGGER.error("Interrupted while disconnecting", e);
            Thread.currentThread().interrupt();
        }
        if (robustDataSendManager != null) {
            robustDataSendManager.close();
        }
        robustDataReceiveThread = null;
        robustDataSendManager = null;
        cyclicBarrier = null;
        dataPacketBuffer.clearAll();
        connected = false;
        LOGGER.info("{} disconnected", ownParty);
    }
}
