package edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.*;
import edu.alibaba.mpc4j.s2pc.pjc.pid.AbstractPidParty;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21.Gmr21MpPidPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GMR21多点PID协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/5/16
 */
public class Gmr21MpPidClient<T> extends AbstractPidParty<T> {
    /**
     * 多点OPRF接收方
     */
    private final MpOprfReceiver mpOprfReceiver;
    /**
     * 多点OPRF发送方
     */
    private final MpOprfSender mpOprfSender;
    /**
     * PSU客户端
     */
    private final PsuClient psuClient;
    /**
     * PID映射函数
     */
    private Hash pidMap;
    /**
     * {F_{k_A}(y) | y ∈ Y'å}
     */
    private MpOprfReceiverOutput kaOprfOutput;
    /**
     * k_B
     */
    private MpOprfSenderOutput kbOprfKey;
    /**
     * 客户端元素字节数组
     */
    private byte[][] clientElementByteArrays;

    public Gmr21MpPidClient(Rpc clientRpc, Party serverParty, Gmr21MpPidConfig config) {
        super(Gmr21MpPidPtoDesc.getInstance(), clientRpc, serverParty, config);
        mpOprfReceiver = OprfFactory.createMpOprfReceiver(clientRpc, serverParty, config.getMpOprfConfig());
        addSubPto(mpOprfReceiver);
        mpOprfSender = OprfFactory.createMpOprfSender(clientRpc, serverParty, config.getMpOprfConfig());
        addSubPto(mpOprfSender);
        psuClient = PsuFactory.createClient(clientRpc, serverParty, config.getPsuConfig());
        addSubPto(psuClient);
    }

    @Override
    public void init(int maxOwnElementSetSize, int maxOtherElementSetSize) throws MpcAbortException {
        setInitInput(maxOwnElementSetSize, maxOtherElementSetSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        mpOprfReceiver.init(maxOwnElementSetSize);
        mpOprfSender.init(maxOtherElementSetSize);
        psuClient.init(maxOwnElementSetSize, maxOtherElementSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PidPartyOutput<T> pid(Set<T> ownElementSet, int otherElementSetSize) throws MpcAbortException {
        setPtoInput(ownElementSet, otherElementSetSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int pidByteLength = PidUtils.getPidByteLength(this.otherElementSetSize, ownElementSetSize);
        pidMap = HashFactory.createInstance(envType, pidByteLength);
        clientElementByteArrays = ownElementArrayList.stream()
            .map(ObjectUtils::objectToByteArray)
            .toArray(byte[][]::new);
        // Alice and Bob invoke the OPRF functionality F_{oprf}.
        // Bob acts as receiver with input Y ′ and receives {F_{k_A}(y) | y ∈ Y'}.
        kaOprfOutput = mpOprfReceiver.oprf(clientElementByteArrays);
        // Alice and Bob invoke another OPRF functionality F_{oprf}.
        // Bob acts as sender and receives a PRF key k_B
        kbOprfKey = mpOprfSender.oprf(otherElementSetSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, oprfTime);

        stopWatch.start();
        // Bob computes Pid(y_i)
        Map<ByteBuffer, T> clientPidMap = generateClientPidMap();
        stopWatch.stop();
        long pidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, pidMapTime);

        stopWatch.start();
        // The parties invoke F_{psu}, with inputs {R_B(x) | y ∈ Y} for Bob
        Set<ByteBuffer> pidSet = psuClient.psu(clientPidMap.keySet(), otherElementSetSize, pidByteLength);
        stopWatch.stop();
        long psuTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, psuTime);

        stopWatch.start();
        // Bob sends union
        List<byte[]> unionPayload = pidSet.stream().map(ByteBuffer::array).collect(Collectors.toList());
        DataPacketHeader unionHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_UNION.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(unionHeader, unionPayload));
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, unionTime);

        logPhaseInfo(PtoState.PTO_END);
        return new PidPartyOutput<>(pidByteLength, pidSet, clientPidMap);
    }

    private Map<ByteBuffer, T> generateClientPidMap() {
        IntStream clientElementIndexStream = IntStream.range(0, ownElementSetSize);
        clientElementIndexStream = parallel ? clientElementIndexStream.parallel() : clientElementIndexStream;
        return clientElementIndexStream
            .boxed()
            .collect(Collectors.toMap(
                    index -> {
                        byte[] y = clientElementByteArrays[index];
                        byte[] pid0 = pidMap.digestToBytes(kaOprfOutput.getPrf(index));
                        byte[] pid1 = pidMap.digestToBytes(kbOprfKey.getPrf(y));
                        BytesUtils.xori(pid0, pid1);
                        return ByteBuffer.wrap(pid0);
                    },
                    index -> ownElementArrayList.get(index)
                )
            );
    }
}
