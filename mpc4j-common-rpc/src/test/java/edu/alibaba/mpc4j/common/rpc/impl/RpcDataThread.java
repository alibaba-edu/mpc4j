package edu.alibaba.mpc4j.common.rpc.impl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcTestUtils;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * 额外信息数据包发送和接收测试线程。
 *
 * @author Weiran Liu
 * @date 2021/12/11
 */
class RpcDataThread extends Thread {
    /**
     * 多条数据数量
     */
    private static final int MULTI_DATA_PACKET_NUM = 5;
    /**
     * 远程通信接口
     */
    private final Rpc rpc;
    /**
     * 任务ID
     */
    private final long taskId;
    /**
     * 发送的空数据包
     */
    private final Set<DataPacket> emptySendDataPacketSet;
    /**
     * 接收的空数据包
     */
    private final Set<DataPacket> emptyReceivedDataPacketSet;
    /**
     * 发送的长度为0的数据包
     */
    private final Set<DataPacket> zeroLengthSendDataPacketSet;
    /**
     * 接收的长度为0的数据包
     */
    private final Set<DataPacket> zeroLengthReceivedDataPacketSet;
    /**
     * 发送的单数据包
     */
    private final Set<DataPacket> singleSendDataPacketSet;
    /**
     * 接收的单数据包
     */
    private final Set<DataPacket> singleReceivedDataPacketSet;
    /**
     * 发送的额外信息数据包
     */
    private final Set<DataPacket> extraInfoSendDataPacketSet;
    /**
     * 接收的额外信息数据包
     */
    private final Set<DataPacket> extraInfoReceivedDataPacketSet;

    RpcDataThread(long taskId, Rpc rpc) {
        this.taskId = taskId;
        this.rpc = rpc;
        emptySendDataPacketSet = new HashSet<>();
        emptyReceivedDataPacketSet = new HashSet<>();
        zeroLengthSendDataPacketSet = new HashSet<>();
        zeroLengthReceivedDataPacketSet = new HashSet<>();
        singleSendDataPacketSet = new HashSet<>();
        singleReceivedDataPacketSet = new HashSet<>();
        extraInfoSendDataPacketSet = new HashSet<>();
        extraInfoReceivedDataPacketSet = new HashSet<>();
    }

    Set<DataPacket> getEmptySendDataPacketSet() {
        return emptySendDataPacketSet;
    }

    Set<DataPacket> getEmptyReceivedDataPacketSet() {
        return emptyReceivedDataPacketSet;
    }

    Set<DataPacket> getZeroLengthSendDataPacketSet() {
        return zeroLengthSendDataPacketSet;
    }

    Set<DataPacket> getZeroLengthReceivedDataPacketSet() {
        return zeroLengthReceivedDataPacketSet;
    }

    Set<DataPacket> getSingleSendDataPacketSet() {
        return singleSendDataPacketSet;
    }

    Set<DataPacket> getSingleReceivedDataPacketSet() {
        return singleReceivedDataPacketSet;
    }

    Set<DataPacket> getExtraInfoSendDataPacketSet() {
        return extraInfoSendDataPacketSet;
    }

    Set<DataPacket> getExtraInfoReceivedDataPacketSet() {
        return extraInfoReceivedDataPacketSet;
    }

    @Override
    public void run() {
        rpc.connect();
        // 发送和接收空数据包
        emptyDataPacket();
        rpc.synchronize();
        // 发送和接收长度为0的数据包
        zeroLengthDataPacket();
        rpc.synchronize();
        // 发送和接收包含单条数据的数据包
        singleDataPacket();
        rpc.synchronize();
        // 发送和接收包含额外信息的多条数据数据包
        extraInfoDataPacket();
        rpc.synchronize();
        rpc.reset();
        rpc.disconnect();
    }

    private void emptyDataPacket() {
        // 发送空数据包
        for (Party party : rpc.getPartySet()) {
            // 不测试自己给自己发送数据包
            if (!party.equals(rpc.ownParty())) {
                DataPacketHeader header = new DataPacketHeader(
                    taskId, RpcTestPtoDesc.getInstance().getPtoId(), RpcTestPtoDesc.PtoStep.EMPTY.ordinal(),
                    rpc.ownParty().getPartyId(), party.getPartyId()
                );
                DataPacket dataPacket = DataPacket.fromByteArrayList(header, new LinkedList<>());
                emptySendDataPacketSet.add(dataPacket);
                rpc.send(dataPacket);
            }
        }
        // 接收空数据包
        for (Party party : rpc.getPartySet()) {
            // 不测试自己给自己发送数据包
            if (!party.equals(rpc.ownParty())) {
                DataPacketHeader header = new DataPacketHeader(
                    taskId, RpcTestPtoDesc.getInstance().getPtoId(), RpcTestPtoDesc.PtoStep.EMPTY.ordinal(),
                    party.getPartyId(), rpc.ownParty().getPartyId()
                );
                DataPacket dataPacket = rpc.receive(header);
                emptyReceivedDataPacketSet.add(dataPacket);
            }
        }
    }

    private void zeroLengthDataPacket() {
        // 发送长度为0的数据包
        for (Party party : rpc.getPartySet()) {
            // 不测试自己给自己发送数据包
            if (!party.equals(rpc.ownParty())) {
                DataPacketHeader header = new DataPacketHeader(
                    taskId, RpcTestPtoDesc.getInstance().getPtoId(), RpcTestPtoDesc.PtoStep.ZERO_LENGTH.ordinal(),
                    rpc.ownParty().getPartyId(), party.getPartyId()
                );
                List<byte[]> payload = new LinkedList<>();
                payload.add(RpcTestUtils.EMPTY_BYTE_ARRAY);
                DataPacket dataPacket = DataPacket.fromByteArrayList(header, payload);
                zeroLengthSendDataPacketSet.add(dataPacket);
                rpc.send(dataPacket);
            }
        }
        // 接收空数据包
        for (Party party : rpc.getPartySet()) {
            // 不测试自己给自己发送数据包
            if (!party.equals(rpc.ownParty())) {
                DataPacketHeader header = new DataPacketHeader(
                    taskId, RpcTestPtoDesc.getInstance().getPtoId(), RpcTestPtoDesc.PtoStep.ZERO_LENGTH.ordinal(),
                    party.getPartyId(), rpc.ownParty().getPartyId()
                );
                DataPacket dataPacket = rpc.receive(header);
                zeroLengthReceivedDataPacketSet.add(dataPacket);
            }
        }
    }

    private void singleDataPacket() {
        // 发送单条数据
        for (Party party : rpc.getPartySet()) {
            // 不测试自己给自己发送数据包
            if (!party.equals(rpc.ownParty())) {
                DataPacketHeader header = new DataPacketHeader(
                    taskId, RpcTestPtoDesc.getInstance().getPtoId(), RpcTestPtoDesc.PtoStep.SINGLE.ordinal(),
                    rpc.ownParty().getPartyId(), party.getPartyId()
                );
                List<byte[]> payload = new LinkedList<>();
                payload.add((rpc.ownParty().getPartyName() + " -> " + party.getPartyName() + "：Default")
                    .getBytes(StandardCharsets.UTF_8)
                );
                DataPacket dataPacket = DataPacket.fromByteArrayList(header, payload);
                singleSendDataPacketSet.add(dataPacket);
                rpc.send(dataPacket);
            }
        }
        // 接收单条数据
        for (Party party : rpc.getPartySet()) {
            // 不测试自己给自己发送数据包
            if (!party.equals(rpc.ownParty())) {
                DataPacketHeader header = new DataPacketHeader(
                    taskId, RpcTestPtoDesc.getInstance().getPtoId(), RpcTestPtoDesc.PtoStep.SINGLE.ordinal(),
                    party.getPartyId(), rpc.ownParty().getPartyId()
                );
                DataPacket dataPacket = rpc.receive(header);
                singleReceivedDataPacketSet.add(dataPacket);
            }
        }
    }

    private void extraInfoDataPacket() {
        // 发送额外信息数据
        for (Party party : rpc.getPartySet()) {
            // 不测试自己给自己发送数据包
            if (!party.equals(rpc.ownParty())) {
                IntStream.range(0, MULTI_DATA_PACKET_NUM).forEach(packetIndex -> {
                    List<byte[]> payload = new LinkedList<>();
                    payload.add((rpc.ownParty().getPartyName() + " -> " + party.getPartyName() + "：" + packetIndex)
                        .getBytes(StandardCharsets.UTF_8)
                    );
                    DataPacketHeader header = new DataPacketHeader(
                        taskId, RpcTestPtoDesc.getInstance().getPtoId(), RpcTestPtoDesc.PtoStep.EXTRA_INFO.ordinal(), packetIndex,
                        rpc.ownParty().getPartyId(), party.getPartyId()
                    );
                    DataPacket dataPacket = DataPacket.fromByteArrayList(header, payload);
                    extraInfoSendDataPacketSet.add(dataPacket);
                    rpc.send(dataPacket);
                });
            }
        }
        // 接收额外信息数据
        for (Party party : rpc.getPartySet()) {
            // 不测试自己给自己发送数据包
            if (!party.equals(rpc.ownParty())) {
                IntStream.range(0, MULTI_DATA_PACKET_NUM).forEach(packetIndex -> {
                    DataPacketHeader header = new DataPacketHeader(
                        taskId, RpcTestPtoDesc.getInstance().getPtoId(), RpcTestPtoDesc.PtoStep.EXTRA_INFO.ordinal(), packetIndex,
                        party.getPartyId(), rpc.ownParty().getPartyId()
                    );
                    DataPacket dataPacket = rpc.receive(header);
                    extraInfoReceivedDataPacketSet.add(dataPacket);
                });
            }
        }
    }
}
