package edu.alibaba.mpc4j.common.rpc.impl.netty;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.NettyRpcProtobuf;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.NettyRpcProtobuf.DataPacketProto;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.NettyRpcProtobuf.DataPacketProto.HeaderProto;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.NettyRpcProtobuf.DataPacketProto.PayloadProto;
import edu.alibaba.mpc4j.common.rpc.impl.netty.protobuf.NettyRpcProtobuf.DataPacketProto.TypeProto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketBuffer;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.rpc.utils.PayloadType;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class NettyRpc implements Rpc {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRpc.class);
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
     * CyclicBarrier，用于多线程同步
     */
    private final CyclicBarrier cyclicBarrier;
    /**
     * 数据接收线程
     */
    private DataReceiveThread dataReceiveThread;
    /**
     * 数据发送管理器
     */
    private DataSendManager dataSendManager;
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
    public NettyRpc(NettyParty ownParty, Set<NettyParty> partySet) {
        // 所有参与方的数量必须大于1
        Preconditions.checkArgument(partySet.size() > 1, "Party set size must be greater than 1");
        // 参与方自身必须在所有参与方之中
        Preconditions.checkArgument(partySet.contains(ownParty), "Party set must contain own party");
        this.ownParty = ownParty;
        ownPartyId = ownParty.getPartyId();
        // 按照参与方索引值，将参与方信息插入到ID映射中
        partyIdHashMap = new HashMap<>();
        partySet.forEach(party -> partyIdHashMap.put(party.getPartyId(), party));
        dataPacketNum = 0;
        payloadByteLength = 0;
        sendByteLength = 0;
        dataReceiveThread = null;
        // 用于父线程和server子线程的同步，parties设置成2
        cyclicBarrier = new CyclicBarrier(2);
        dataPacketBuffer = new DataPacketBuffer();
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
        // 先开启数据接收服务
        dataReceiveThread = new DataReceiveThread(ownParty, cyclicBarrier, dataPacketBuffer);
        dataReceiveThread.start();
        // 再开启数据发送服务
        dataSendManager = new DataSendManager();
        partyIdHashMap.keySet().stream().sorted().forEach(otherPartyId -> {
            if (otherPartyId < ownPartyId) {
                // 如果对方排序比自己小，则自己是client，先给对方发送连接信息
                DataPacketHeader clientConnectHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, NettyPtoDesc.getInstance().getPtoId(), NettyPtoDesc.StepEnum.CLIENT_CONNECT.ordinal(),
                    ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(clientConnectHeader, new LinkedList<>()));
                LOGGER.debug(
                    "{} requests connection with {}",
                    partyIdHashMap.get(ownPartyId), partyIdHashMap.get(otherPartyId)
                );
                // 再获得对方的回复
                DataPacketHeader serverConnectHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, NettyPtoDesc.getInstance().getPtoId(), NettyPtoDesc.StepEnum.SERVER_CONNECT.ordinal(),
                    otherPartyId, ownPartyId
                );
                receive(serverConnectHeader);
                LOGGER.debug(
                    "{} successfully make connection with {}",
                    partyIdHashMap.get(ownPartyId), partyIdHashMap.get(otherPartyId)
                );
            } else if (otherPartyId > ownPartyId) {
                // 如果对方排序比自己大，则自己是server，先接收对方的连接信息
                DataPacketHeader clientConnectHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, NettyPtoDesc.getInstance().getPtoId(), NettyPtoDesc.StepEnum.CLIENT_CONNECT.ordinal(),
                    otherPartyId, ownPartyId
                );
                LOGGER.debug(
                    "{} requests being connected with {}",
                    partyIdHashMap.get(ownPartyId), partyIdHashMap.get(otherPartyId)
                );
                receive(clientConnectHeader);
                // 再回复给对方
                DataPacketHeader serverConnectHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, NettyPtoDesc.getInstance().getPtoId(), NettyPtoDesc.StepEnum.SERVER_CONNECT.ordinal(),
                    ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(serverConnectHeader, new LinkedList<>()));
                LOGGER.debug(
                    "{} successfully make connection with {}",
                    partyIdHashMap.get(ownPartyId), partyIdHashMap.get(otherPartyId)
                );
            }
        });
        LOGGER.info("{} connected", ownParty);
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
        DataPacketProto dataPacketProto = NettyRpcProtobuf.DataPacketProto
            .newBuilder()
            .setHeaderProto(headerProto)
            .setTypeProto(typeProto)
            .setPayloadProto(payloadProto)
            .build();
        payloadByteLength += dataPacket.getPayload().stream().mapToInt(data -> data.length).sum();
        sendByteLength += dataPacketProto.getSerializedSize();
        dataPacketNum++;
        dataSendManager.sendData(partyIdHashMap.get(header.getReceiverId()), dataPacketProto);
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
    public DataPacket receiveAny() {
        try {
            // 尝试从缓存区中读取数据
            return dataPacketBuffer.take(ownPartyId);
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
                    Long.MAX_VALUE - ownPartyId, NettyPtoDesc.getInstance().getPtoId(), NettyPtoDesc.StepEnum.CLIENT_SYNCHRONIZE.ordinal(),
                    ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(clientSynchronizeHeader, new LinkedList<>()));
                // 获得对方的回复
                DataPacketHeader serverSynchronizeHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, NettyPtoDesc.getInstance().getPtoId(), NettyPtoDesc.StepEnum.SERVER_SYNCHRONIZE.ordinal(),
                    otherPartyId, ownPartyId
                );
                receive(serverSynchronizeHeader);
            } else if (otherPartyId > ownPartyId) {
                // 如果对方排序比自己大，则自己是server
                DataPacketHeader clientSynchronizeHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, NettyPtoDesc.getInstance().getPtoId(), NettyPtoDesc.StepEnum.CLIENT_SYNCHRONIZE.ordinal(),
                    otherPartyId, ownPartyId
                );
                receive(clientSynchronizeHeader);
                DataPacketHeader serverSynchronizeHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, NettyPtoDesc.getInstance().getPtoId(), NettyPtoDesc.StepEnum.SERVER_SYNCHRONIZE.ordinal(),
                    ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(serverSynchronizeHeader, new LinkedList<>()));
            }
        });
        LOGGER.info("{} synchronized", ownParty);
    }

    @Override
    public void disconnect() {
        // 对参与方进行排序，所有在自己之前的自己作为client、所有在自己之后的自己作为server
        partyIdHashMap.keySet().stream().sorted().forEach(otherPartyId -> {
            if (otherPartyId < ownPartyId) {
                // 如果对方排序比自己小，则自己是client，需要给对方发送连接信息
                DataPacketHeader clientFinishHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, NettyPtoDesc.getInstance().getPtoId(), NettyPtoDesc.StepEnum.CLIENT_FINISH.ordinal(),
                    ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(clientFinishHeader, new LinkedList<>()));
                // 获得对方的回复
                DataPacketHeader serverFinishHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, NettyPtoDesc.getInstance().getPtoId(), NettyPtoDesc.StepEnum.SERVER_FINISH.ordinal(),
                    otherPartyId, ownPartyId
                );
                receive(serverFinishHeader);
            } else if (otherPartyId > ownPartyId) {
                // 如果对方排序比自己大，则自己是server
                DataPacketHeader clientFinishHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, NettyPtoDesc.getInstance().getPtoId(), NettyPtoDesc.StepEnum.CLIENT_FINISH.ordinal(),
                    otherPartyId, ownPartyId
                );
                receive(clientFinishHeader);
                DataPacketHeader serverFinishHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, NettyPtoDesc.getInstance().getPtoId(), NettyPtoDesc.StepEnum.SERVER_FINISH.ordinal(),
                    ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(serverFinishHeader, new LinkedList<>()));
            }
        });
        try {
            // 关闭数据接收服务
            dataReceiveThread.close();
            // 通过CyclicBarrier变量与主线程进行同步
            cyclicBarrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
        LOGGER.info("{} disconnected", ownParty);
    }
}
