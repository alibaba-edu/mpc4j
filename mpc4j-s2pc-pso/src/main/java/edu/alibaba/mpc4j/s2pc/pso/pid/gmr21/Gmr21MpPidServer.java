package edu.alibaba.mpc4j.s2pc.pso.pid.gmr21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pso.oprf.*;
import edu.alibaba.mpc4j.s2pc.pso.pid.AbstractPidParty;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidPartyOutput;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidUtils;
import edu.alibaba.mpc4j.s2pc.pso.pid.gmr21.Gmr21MpPidPtoDesc.PtoStep;
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
        mpOprfSender.addLogLevel();
        mpOprfReceiver = OprfFactory.createMpOprfReceiver(serverRpc, clientParty, config.getMpOprfConfig());
        mpOprfReceiver.addLogLevel();
        psuServer = PsuFactory.createServer(serverRpc, clientParty, config.getPsuConfig());
        psuServer.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        mpOprfSender.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        mpOprfReceiver.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
        psuServer.setTaskId(taskIdPrf.getLong(2, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        mpOprfSender.setParallel(parallel);
        mpOprfReceiver.setParallel(parallel);
        psuServer.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        mpOprfSender.addLogLevel();
        mpOprfReceiver.addLogLevel();
        psuServer.addLogLevel();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        mpOprfSender.init(maxClientElementSize);
        mpOprfReceiver.init(maxServerElementSize);
        psuServer.init(maxServerElementSize, maxClientElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Server Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public PidPartyOutput<T> pid(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        info("{}{} Server begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int pidByteLength = PidUtils.getPidByteLength(ownSetSize, otherSetSize);
        pidMap = HashFactory.createInstance(envType, pidByteLength);
        serverElementByteArrays = ownElementArrayList.stream()
            .map(ObjectUtils::objectToByteArray)
            .toArray(byte[][]::new);
        // Alice and Bob invoke the OPRF functionality F_{oprf}.
        // Alice acts as sender and receives a PRF key k_A
        kaOprfKey = mpOprfSender.oprf(clientElementSize);
        // Alice and Bob invoke another OPRF functionality F_{oprf}.
        // Alice acts as receiver with input X and receives {F_{k_B}(x) | x ∈ X}.
        kbOprfOutput = mpOprfReceiver.oprf(serverElementByteArrays);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 1/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

        stopWatch.start();
        // Alice computes Pid(x_i)
        Map<ByteBuffer, T> serverPidMap = generateServerPidMap();
        stopWatch.stop();
        long pidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 2/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pidMapTime);

        stopWatch.start();
        // The parties invoke F_{psu}, with inputs {R_A(x) | x ∈ X} for Alice
        psuServer.psu(serverPidMap.keySet(), clientElementSize, pidByteLength);
        stopWatch.stop();
        long psuTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 3/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), psuTime);

        stopWatch.start();
        // Alice receives union
        DataPacketHeader unionHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_UNION.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> unionPayload = rpc.receive(unionHeader).getPayload();
        MpcAbortPreconditions.checkArgument(unionPayload.size() >= ownSetSize);
        Set<ByteBuffer> pidSet = unionPayload.stream()
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 4/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), unionTime);

        info("{}{} Server end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return new PidPartyOutput<>(pidByteLength, pidSet, serverPidMap);
    }

    private Map<ByteBuffer, T> generateServerPidMap() {
        IntStream serverElementIndexStream = IntStream.range(0, ownSetSize);
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
