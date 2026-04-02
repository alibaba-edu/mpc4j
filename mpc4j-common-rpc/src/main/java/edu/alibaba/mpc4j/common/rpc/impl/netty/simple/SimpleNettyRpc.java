package edu.alibaba.mpc4j.common.rpc.impl.netty.simple;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyParty;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.SimpleNettyRpcProtobuf;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.SimpleNettyRpcProtobuf.DataPacketProto;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.SimpleNettyRpcProtobuf.DataPacketProto.HeaderProto;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.SimpleNettyRpcProtobuf.DataPacketProto.PayloadProto;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.SimpleNettyRpcProtobuf.DataPacketProto.TypeProto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketBuffer;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.rpc.utils.PayloadType;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

/**
 * 用Netty实现的RPC。
 *
 * @author Feng Qing, Weiran Liu
 * @date 2020/10/12
 */
public class SimpleNettyRpc implements Rpc {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleNettyRpc.class);
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
    private SimpleDataReceiveThread simpleDataReceiveThread;
    /**
     * 数据发送管理器
     */
    private SimpleDataSendManager simpleDataSendManager;
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
     * 构建NettyRPC。
     *
     * @param ownParty 参与方信息。
     */
    public SimpleNettyRpc(NettyParty ownParty, Set<NettyParty> partySet) {
        // 所有参与方的数量必须大于1
        Preconditions.checkArgument(partySet.size() > 1, "Party set size must be greater than 1");
        // 参与方自身必须在所有参与方之中
        Preconditions.checkArgument(partySet.contains(ownParty), "Party set must contain own party");
        // TEST whether the current port is in use
        testPortInUse(ownParty.getPort());
        this.ownParty = ownParty;
        ownPartyId = ownParty.getPartyId();
        // 按照参与方索引值，将参与方信息插入到ID映射中
        partyIdHashMap = new HashMap<>();
        partySet.forEach(party -> partyIdHashMap.put(party.getPartyId(), party));
        dataPacketNum = 0;
        payloadByteLength = 0;
        sendByteLength = 0;
        // 初始化状态：未连接
        connected = false;
        cyclicBarrier = null;
        simpleDataReceiveThread = null;
        simpleDataSendManager = null;
        dataPacketBuffer = new DataPacketBuffer();
    }

    /**
     * If the current port is in use, exit.
     *
     * @param port port.
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
        // 防止重复connect
        if (connected) {
            LOGGER.warn("{} already connected, skip connect()", ownParty);
            return;
        }
        // 每次connect()创建新的CyclicBarrier，用于父线程和server子线程的同步，parties设置成2
        cyclicBarrier = new CyclicBarrier(2);
        // 先开启数据接收服务
        simpleDataReceiveThread = new SimpleDataReceiveThread(ownParty, cyclicBarrier, dataPacketBuffer);
        simpleDataReceiveThread.start();
        // 再开启数据发送服务
        simpleDataSendManager = new SimpleDataSendManager();
        partyIdHashMap.keySet().stream().sorted().forEach(otherPartyId -> {
            if (otherPartyId < ownPartyId) {
                // 如果对方排序比自己小，则自己是client，先给对方发送连接信息
                DataPacketHeader clientConnectHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, SimpleNettyPtoDesc.getInstance().getPtoId(), SimpleNettyPtoDesc.StepEnum.CLIENT_CONNECT.ordinal(),
                    ownPartyId, otherPartyId
                );
                DataPacketHeader serverConnectHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, SimpleNettyPtoDesc.getInstance().getPtoId(), SimpleNettyPtoDesc.StepEnum.SERVER_CONNECT.ordinal(),
                    otherPartyId, ownPartyId
                );
                DataPacketHeader clientConfirmHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, SimpleNettyPtoDesc.getInstance().getPtoId(), SimpleNettyPtoDesc.StepEnum.CLIENT_CONFIRM.ordinal(),
                    ownPartyId, otherPartyId
                );

                send(DataPacket.fromByteArrayList(clientConnectHeader, new LinkedList<>()));
                // 再获得对方的回复
                while (receiveWithSleep(serverConnectHeader) == null) {
                    LOGGER.info(
                        "{} requests connection with {}",
                        partyIdHashMap.get(ownPartyId), partyIdHashMap.get(otherPartyId)
                    );
                    send(DataPacket.fromByteArrayList(clientConnectHeader, new LinkedList<>()));
                }
                send(DataPacket.fromByteArrayList(clientConfirmHeader, new LinkedList<>()));
                dataPacketBuffer.clearBuffer(serverConnectHeader);
                LOGGER.info(
                    "{} successfully make connection with {}",
                    partyIdHashMap.get(ownPartyId), partyIdHashMap.get(otherPartyId)
                );
            } else if (otherPartyId > ownPartyId) {
                // 如果对方排序比自己大，则自己是server，先接收对方的连接信息
                DataPacketHeader clientConnectHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, SimpleNettyPtoDesc.getInstance().getPtoId(), SimpleNettyPtoDesc.StepEnum.CLIENT_CONNECT.ordinal(),
                    otherPartyId, ownPartyId
                );
                DataPacketHeader serverConnectHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, SimpleNettyPtoDesc.getInstance().getPtoId(), SimpleNettyPtoDesc.StepEnum.SERVER_CONNECT.ordinal(),
                    ownPartyId, otherPartyId
                );
                DataPacketHeader clientConfirmHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, SimpleNettyPtoDesc.getInstance().getPtoId(), SimpleNettyPtoDesc.StepEnum.CLIENT_CONFIRM.ordinal(),
                    otherPartyId, ownPartyId
                );

                while (receiveWithSleep(clientConnectHeader) == null) {
                    LOGGER.info(
                        "{} requests being connected with {}",
                        partyIdHashMap.get(ownPartyId), partyIdHashMap.get(otherPartyId)
                    );
                }

                send(DataPacket.fromByteArrayList(serverConnectHeader, new LinkedList<>()));
                while (receiveWithSleep(clientConfirmHeader) == null) {
                    LOGGER.info(
                        "{} requests confirm from {}",
                        partyIdHashMap.get(ownPartyId), partyIdHashMap.get(otherPartyId)
                    );
                    send(DataPacket.fromByteArrayList(serverConnectHeader, new LinkedList<>()));
                }
                dataPacketBuffer.clearBuffer(clientConnectHeader);
                dataPacketBuffer.clearBuffer(clientConfirmHeader);
                LOGGER.info(
                    "{} successfully make connection with {}",
                    partyIdHashMap.get(ownPartyId), partyIdHashMap.get(otherPartyId)
                );
            }
        });
        // 标记为已连接
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
        Preconditions.checkArgument(
            ownPartyId == header.getReceiverId(), "Receiver ID must be %s", ownPartyId
        );
        Preconditions.checkArgument(
            partyIdHashMap.containsKey(header.getSenderId()),
            "Party set does not contain Sender ID = %s", header.getSenderId()
        );
        try {
            Thread.sleep(100);
            return dataPacketBuffer.takeImmediately(header);
        } catch (InterruptedException e) {
            // 线程中断，不需要等待，直接返回空
            return null;
        }
    }

    @Override
    public void send(DataPacket dataPacket) {
        DataPacketHeader header = dataPacket.getHeader();
        PayloadType payloadType = dataPacket.getPayloadType();
        List<byte[]> payload = dataPacket.getPayload();
        Preconditions.checkArgument(
            ownPartyId == header.getSenderId(), "Sender ID must be %s", ownPartyId
        );
        Preconditions.checkArgument(
            partyIdHashMap.containsKey(header.getReceiverId()),
            "Party set does not contain Receiver ID = %s", header.getReceiverId()
        );
        // package header
        HeaderProto headerProto = HeaderProto.newBuilder()
            .setEncodeTaskId(header.getEncodeTaskId())
            .setPtoId(header.getPtoId())
            .setStepId(header.getStepId())
            .setExtraInfo(header.getExtraInfo())
            .setSenderId(header.getSenderId())
            .setReceiverId(header.getReceiverId())
            .build();
        // package type
        TypeProto typeProto = TypeProto.newBuilder()
            .setTypeId(payloadType.ordinal())
            .build();
        // package payload
        List<ByteString> payloadByteStringList;
        switch (payloadType) {
            case NORMAL:
            case EMPTY:
            case SINGLETON:
                payloadByteStringList = payload.stream()
                    .map(ByteString::copyFrom)
                    .collect(Collectors.toList());
                break;
            case EQUAL_SIZE:
                int length = dataPacket.getEqualLength();
                payloadByteStringList = new LinkedList<>();
                payloadByteStringList.add(ByteString.copyFrom(IntUtils.intToByteArray(length)));
                payloadByteStringList.add(ByteString.copyFrom(SerializeUtils.compressEqual(payload, length)));
                break;
            default:
                throw new IllegalStateException("Invalid " + PayloadType.class.getSimpleName() + ": " + payloadType);
        }
        PayloadProto payloadProto = PayloadProto.newBuilder()
            .addAllPayloadBytes(payloadByteStringList)
            .build();
        // package data packet
        DataPacketProto dataPacketProto = SimpleNettyRpcProtobuf.DataPacketProto
            .newBuilder()
            .setHeaderProto(headerProto)
            .setTypeProto(typeProto)
            .setPayloadProto(payloadProto)
            .build();
        payloadByteLength += dataPacket.getPayload().stream().mapToLong(data -> data.length).sum();
        sendByteLength += dataPacketProto.getSerializedSize();
        dataPacketNum++;
        simpleDataSendManager.sendData(partyIdHashMap.get(header.getReceiverId()), dataPacketProto);
    }

    @Override
    public DataPacket receive(DataPacketHeader header) {
        Preconditions.checkArgument(
            ownPartyId == header.getReceiverId(), "Receiver ID must be %s", ownPartyId
        );
        Preconditions.checkArgument(
            partyIdHashMap.containsKey(header.getSenderId()),
            "Party set does not contain Sender ID = %s", header.getSenderId()
        );
        try {
            // 尝试从缓存区中读取数据
            return dataPacketBuffer.take(header);
        } catch (InterruptedException e) {
            // 线程中断，不需要等待，直接返回空
            return null;
        }
    }

    @Override
    public DataPacket receiveAny(int ptoId) {
        try {
            return dataPacketBuffer.take(ownPartyId, ptoId);
        } catch (InterruptedException e) {
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
        // 对参与方进行排序，所有在自己之前的自己作为client、所有在自己之后的自己作为server
        partyIdHashMap.keySet().stream().sorted().forEach(otherPartyId -> {
            if (otherPartyId < ownPartyId) {
                // 如果对方排序比自己小，则自己是client，需要给对方发送同步信息
                DataPacketHeader clientSynchronizeHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, SimpleNettyPtoDesc.getInstance().getPtoId(), SimpleNettyPtoDesc.StepEnum.CLIENT_SYNCHRONIZE.ordinal(),
                    ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(clientSynchronizeHeader, new LinkedList<>()));
                // 获得对方的回复
                DataPacketHeader serverSynchronizeHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, SimpleNettyPtoDesc.getInstance().getPtoId(), SimpleNettyPtoDesc.StepEnum.SERVER_SYNCHRONIZE.ordinal(),
                    otherPartyId, ownPartyId
                );
                receive(serverSynchronizeHeader);
            } else if (otherPartyId > ownPartyId) {
                // 如果对方排序比自己大，则自己是server
                DataPacketHeader clientSynchronizeHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, SimpleNettyPtoDesc.getInstance().getPtoId(), SimpleNettyPtoDesc.StepEnum.CLIENT_SYNCHRONIZE.ordinal(),
                    otherPartyId, ownPartyId
                );
                receive(clientSynchronizeHeader);
                DataPacketHeader serverSynchronizeHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, SimpleNettyPtoDesc.getInstance().getPtoId(), SimpleNettyPtoDesc.StepEnum.SERVER_SYNCHRONIZE.ordinal(),
                    ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(serverSynchronizeHeader, new LinkedList<>()));
            }
        });
        LOGGER.info("{} synchronized", ownParty);
    }

    @Override
    public void disconnect() {
        // 防止重复disconnect
        if (!connected) {
            LOGGER.warn("{} already disconnected, skip disconnect()", ownParty);
            return;
        }
        // 对参与方进行排序，所有在自己之前的自己作为client、所有在自己之后的自己作为server
        partyIdHashMap.keySet().stream().sorted().forEach(otherPartyId -> {
            if (otherPartyId < ownPartyId) {
                // 如果对方排序比自己小，则自己是client，需要给对方发送断开连接信息
                DataPacketHeader clientFinishHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, SimpleNettyPtoDesc.getInstance().getPtoId(), SimpleNettyPtoDesc.StepEnum.CLIENT_FINISH.ordinal(),
                    ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(clientFinishHeader, new LinkedList<>()));
                // 获得对方的回复
                DataPacketHeader serverFinishHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, SimpleNettyPtoDesc.getInstance().getPtoId(), SimpleNettyPtoDesc.StepEnum.SERVER_FINISH.ordinal(),
                    otherPartyId, ownPartyId
                );
                receive(serverFinishHeader);
            } else if (otherPartyId > ownPartyId) {
                // 如果对方排序比自己大，则自己是server
                DataPacketHeader clientFinishHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, SimpleNettyPtoDesc.getInstance().getPtoId(), SimpleNettyPtoDesc.StepEnum.CLIENT_FINISH.ordinal(),
                    otherPartyId, ownPartyId
                );
                receive(clientFinishHeader);
                DataPacketHeader serverFinishHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, SimpleNettyPtoDesc.getInstance().getPtoId(), SimpleNettyPtoDesc.StepEnum.SERVER_FINISH.ordinal(),
                    ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(serverFinishHeader, new LinkedList<>()));
            }
        });
        try {
            // 关闭数据接收服务
            simpleDataReceiveThread.close();
            // 通过CyclicBarrier变量与主线程进行同步
            cyclicBarrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            LOGGER.error("Interrupted while disconnecting", e);
            Thread.currentThread().interrupt();
        }
        // 关闭数据发送服务
        if (simpleDataSendManager != null) {
            simpleDataSendManager.close();
        }
        // 清理资源引用，便于GC
        simpleDataReceiveThread = null;
        simpleDataSendManager = null;
        cyclicBarrier = null;
        // 清空数据缓冲区，防止残留数据包干扰下次connect()的握手流程
        dataPacketBuffer.clearAll();
        // 标记为已断开
        connected = false;
        LOGGER.info("{} disconnected", ownParty);
    }
}
