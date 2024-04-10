package edu.alibaba.mpc4j.common.rpc.impl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcTestUtils;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.impl.RpcTestPtoDesc.PtoStep;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
     * number of data in the equal-length data packet
     */
    private static final int EQUAL_LENGTH_BYTE_NUM = 1000;
    /**
     * byte length for equal-length data
     */
    private static final int EQUAL_LENGTH_BYTE_SIZE = 20;
    /**
     * test protocol description
     */
    private static final PtoDesc TEST_PTO_DESC = RpcTestPtoDesc.getInstance();
    /**
     * 远程通信接口
     */
    private final Rpc rpc;
    /**
     * 任务ID
     */
    private final int taskId;
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
     * send equal-length data packet
     */
    private final Set<DataPacket> equalLengthSendDataPacketSet;
    /**
     * receive equal-length data packet
     */
    private final Set<DataPacket> equalLengthReceivedDataPacketSet;
    /**
     * 发送的额外信息数据包
     */
    private final Set<DataPacket> extraInfoSendDataPacketSet;
    /**
     * 接收的额外信息数据包
     */
    private final Set<DataPacket> extraInfoReceivedDataPacketSet;
    /**
     * take any receive data packet
     */
    private final Set<DataPacket> takeAnySendDataPacketSet;
    /**
     * take any sent data packet
     */
    private final Set<DataPacket> takeAnyReceivedDataPacketSet;

    RpcDataThread(int taskId, Rpc rpc) {
        this.taskId = taskId;
        this.rpc = rpc;
        emptySendDataPacketSet = new HashSet<>();
        emptyReceivedDataPacketSet = new HashSet<>();
        zeroLengthSendDataPacketSet = new HashSet<>();
        zeroLengthReceivedDataPacketSet = new HashSet<>();
        singleSendDataPacketSet = new HashSet<>();
        singleReceivedDataPacketSet = new HashSet<>();
        equalLengthSendDataPacketSet = new HashSet<>();
        equalLengthReceivedDataPacketSet = new HashSet<>();
        extraInfoSendDataPacketSet = new HashSet<>();
        extraInfoReceivedDataPacketSet = new HashSet<>();
        takeAnySendDataPacketSet = new HashSet<>();
        takeAnyReceivedDataPacketSet = new HashSet<>();
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

    Set<DataPacket> getEqualLengthSendDataPacketSet() {
        return equalLengthSendDataPacketSet;
    }

    Set<DataPacket> getEqualLengthReceivedDataPacketSet() {
        return equalLengthReceivedDataPacketSet;
    }

    Set<DataPacket> getExtraInfoSendDataPacketSet() {
        return extraInfoSendDataPacketSet;
    }

    Set<DataPacket> getExtraInfoReceivedDataPacketSet() {
        return extraInfoReceivedDataPacketSet;
    }

    Set<DataPacket> getTakeAnySendDataPacketSet() {
        return takeAnySendDataPacketSet;
    }

    Set<DataPacket> getTakeAnyReceivedDataPacketSet() {
        return takeAnyReceivedDataPacketSet;
    }

    @Override
    public void run() {
        // 发送和接收空数据包
        emptyDataPacket();
        rpc.synchronize();
        // 发送和接收长度为0的数据包
        zeroLengthDataPacket();
        rpc.synchronize();
        // 发送和接收包含单条数据的数据包
        singletonDataPacket();
        rpc.synchronize();
        // send and receive equal-length data packet
        equalLengthDataPacket();
        rpc.synchronize();
        // 发送和接收包含额外信息的多条数据数据包
        extraInfoDataPacket();
        rpc.synchronize();
        // 发送多个单条数据发送包并接收
        takeAnyDataPacket();
        rpc.synchronize();
    }

    private void emptyDataPacket() {
        // 发送空数据包
        for (Party party : rpc.getPartySet()) {
            // 不测试自己给自己发送数据包
            if (!party.equals(rpc.ownParty())) {
                DataPacketHeader header = new DataPacketHeader(
                    taskId, TEST_PTO_DESC.getPtoId(), RpcTestPtoDesc.PtoStep.EMPTY.ordinal(),
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
                    taskId, TEST_PTO_DESC.getPtoId(), RpcTestPtoDesc.PtoStep.EMPTY.ordinal(),
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
                    taskId, TEST_PTO_DESC.getPtoId(), RpcTestPtoDesc.PtoStep.ZERO_LENGTH.ordinal(),
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
                    taskId, TEST_PTO_DESC.getPtoId(), RpcTestPtoDesc.PtoStep.ZERO_LENGTH.ordinal(),
                    party.getPartyId(), rpc.ownParty().getPartyId()
                );
                DataPacket dataPacket = rpc.receive(header);
                zeroLengthReceivedDataPacketSet.add(dataPacket);
            }
        }
    }

    private void singletonDataPacket() {
        // 发送单条数据
        for (Party party : rpc.getPartySet()) {
            // 不测试自己给自己发送数据包
            if (!party.equals(rpc.ownParty())) {
                DataPacketHeader header = new DataPacketHeader(
                    taskId, TEST_PTO_DESC.getPtoId(), RpcTestPtoDesc.PtoStep.SINGLETON.ordinal(),
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
                    taskId, TEST_PTO_DESC.getPtoId(), RpcTestPtoDesc.PtoStep.SINGLETON.ordinal(),
                    party.getPartyId(), rpc.ownParty().getPartyId()
                );
                DataPacket dataPacket = rpc.receive(header);
                singleReceivedDataPacketSet.add(dataPacket);
            }
        }
    }

    private void equalLengthDataPacket() {
        SecureRandom secureRandom = new SecureRandom();
        // send equal-length data packet
        for (Party party : rpc.getPartySet()) {
            // do not send to its own
            if (!party.equals(rpc.ownParty())) {
                DataPacketHeader header = new DataPacketHeader(
                    taskId, TEST_PTO_DESC.getPtoId(), PtoStep.EQUAL_LENGTH.ordinal(),
                    rpc.ownParty().getPartyId(), party.getPartyId()
                );
                List<byte[]> payload = IntStream.range(0, EQUAL_LENGTH_BYTE_NUM)
                    .mapToObj(index -> {
                        byte[] data = new byte[EQUAL_LENGTH_BYTE_SIZE];
                        secureRandom.nextBytes(data);
                        return data;
                    })
                    .collect(Collectors.toList());
                DataPacket dataPacket = DataPacket.fromByteArrayList(header, payload);
                equalLengthSendDataPacketSet.add(dataPacket);
                rpc.send(dataPacket);
            }
        }
        // receive equal-length data packet
        for (Party party : rpc.getPartySet()) {
            // do not receive from its own
            if (!party.equals(rpc.ownParty())) {
                DataPacketHeader header = new DataPacketHeader(
                    taskId, TEST_PTO_DESC.getPtoId(), PtoStep.EQUAL_LENGTH.ordinal(),
                    party.getPartyId(), rpc.ownParty().getPartyId()
                );
                DataPacket dataPacket = rpc.receive(header);
                equalLengthReceivedDataPacketSet.add(dataPacket);
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
                        taskId, TEST_PTO_DESC.getPtoId(), RpcTestPtoDesc.PtoStep.EXTRA_INFO.ordinal(), packetIndex,
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
                        taskId, TEST_PTO_DESC.getPtoId(), RpcTestPtoDesc.PtoStep.EXTRA_INFO.ordinal(), packetIndex,
                        party.getPartyId(), rpc.ownParty().getPartyId()
                    );
                    DataPacket dataPacket = rpc.receive(header);
                    extraInfoReceivedDataPacketSet.add(dataPacket);
                });
            }
        }
    }

    private void takeAnyDataPacket() {
        for (int extraInfo = 0; extraInfo < 100; extraInfo++) {
            // 发送单条数据
            for (Party party : rpc.getPartySet()) {
                // 不测试自己给自己发送数据包
                if (!party.equals(rpc.ownParty())) {
                    DataPacketHeader header = new DataPacketHeader(
                        taskId, TEST_PTO_DESC.getPtoId(), RpcTestPtoDesc.PtoStep.TAKE_ANY.ordinal(), extraInfo,
                        rpc.ownParty().getPartyId(), party.getPartyId()
                    );
                    DataPacket dataPacket = DataPacket.fromByteArrayList(header, new LinkedList<>());
                    takeAnySendDataPacketSet.add(dataPacket);
                    rpc.send(dataPacket);
                }
            }
            // 接收任意数据
            for (Party party : rpc.getPartySet()) {
                // 不测试自己给自己发送数据包
                if (!party.equals(rpc.ownParty())) {
                    DataPacket dataPacket = rpc.receiveAny();
                    takeAnyReceivedDataPacketSet.add(dataPacket);
                }
            }
        }
    }
}
