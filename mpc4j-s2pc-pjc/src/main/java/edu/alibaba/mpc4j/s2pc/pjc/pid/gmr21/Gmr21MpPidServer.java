package edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21;

import edu.alibaba.mpc4j.common.rpc.*;
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
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuServer;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GMR21多点PID协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/5/16
 */
public class Gmr21MpPidServer<T> extends AbstractPidParty<T> {
    /**
     * 多点OPRF发送方
     */
    private final MpOprfSender mpOprfSender;
    /**
     * 多点OPRF接收方
     */
    private final MpOprfReceiver mpOprfReceiver;
    /**
     * PSU协议服务端
     */
    private final PsuServer psuServer;
    /**
     * PID映射函数
     */
    private Hash pidMap;
    /**
     * k_A
     */
    private MpOprfSenderOutput kaOprfKey;
    /**
     * {F_{k_B}(x) | x ∈ X}
     */
    private MpOprfReceiverOutput kbOprfOutput;
    /**
     * 服务端元素字节数组
     */
    private byte[][] serverElementByteArrays;

    public Gmr21MpPidServer(Rpc serverRpc, Party clientParty, Gmr21MpPidConfig config) {
        super(Gmr21MpPidPtoDesc.getInstance(), serverRpc, clientParty, config);
        mpOprfSender = OprfFactory.createMpOprfSender(serverRpc, clientParty, config.getMpOprfConfig());
        addSubPto(mpOprfSender);
        mpOprfReceiver = OprfFactory.createMpOprfReceiver(serverRpc, clientParty, config.getMpOprfConfig());
        addSubPto(mpOprfReceiver);
        psuServer = PsuFactory.createServer(serverRpc, clientParty, config.getPsuConfig());
        addSubPto(psuServer);
    }

    @Override
    public void init(int maxOwnElementSetSize, int maxOtherElementSetSize) throws MpcAbortException {
        setInitInput(maxOwnElementSetSize, maxOtherElementSetSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        mpOprfSender.init(maxOtherElementSetSize);
        mpOprfReceiver.init(maxOwnElementSetSize);
        psuServer.init(maxOwnElementSetSize, maxOtherElementSetSize);
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
        int pidByteLength = PidUtils.getPidByteLength(ownElementSetSize, this.otherElementSetSize);
        pidMap = HashFactory.createInstance(envType, pidByteLength);
        serverElementByteArrays = ownElementArrayList.stream()
            .map(ObjectUtils::objectToByteArray)
            .toArray(byte[][]::new);
        // Alice and Bob invoke the OPRF functionality F_{oprf}.
        // Alice acts as sender and receives a PRF key k_A
        kaOprfKey = mpOprfSender.oprf(otherElementSetSize);
        // Alice and Bob invoke another OPRF functionality F_{oprf}.
        // Alice acts as receiver with input X and receives {F_{k_B}(x) | x ∈ X}.
        kbOprfOutput = mpOprfReceiver.oprf(serverElementByteArrays);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, oprfTime);

        stopWatch.start();
        // Alice computes Pid(x_i)
        Map<ByteBuffer, T> serverPidMap = generateServerPidMap();
        stopWatch.stop();
        long pidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, pidMapTime);

        stopWatch.start();
        // The parties invoke F_{psu}, with inputs {R_A(x) | x ∈ X} for Alice
        psuServer.psu(serverPidMap.keySet(), otherElementSetSize, pidByteLength);
        stopWatch.stop();
        long psuTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, psuTime);

        stopWatch.start();
        // Alice receives union
        DataPacketHeader unionHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_UNION.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> unionPayload = rpc.receive(unionHeader).getPayload();
        MpcAbortPreconditions.checkArgument(unionPayload.size() >= ownElementSetSize);
        Set<ByteBuffer> pidSet = unionPayload.stream()
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, unionTime);

        logPhaseInfo(PtoState.PTO_END);
        return new PidPartyOutput<>(pidByteLength, pidSet, serverPidMap);
    }

    private Map<ByteBuffer, T> generateServerPidMap() {
        IntStream serverElementIndexStream = IntStream.range(0, ownElementSetSize);
        serverElementIndexStream = parallel ? serverElementIndexStream.parallel() : serverElementIndexStream;
        return serverElementIndexStream
            .boxed()
            .collect(Collectors.toMap(
                    index -> {
                        byte[] x = serverElementByteArrays[index];
                        byte[] pid0 = pidMap.digestToBytes(kaOprfKey.getPrf(x));
                        byte[] pid1 = pidMap.digestToBytes(kbOprfOutput.getPrf(index));
                        BytesUtils.xori(pid0, pid1);
                        return ByteBuffer.wrap(pid0);
                    },
                    index -> ownElementArrayList.get(index)
                )
            );
    }
}
