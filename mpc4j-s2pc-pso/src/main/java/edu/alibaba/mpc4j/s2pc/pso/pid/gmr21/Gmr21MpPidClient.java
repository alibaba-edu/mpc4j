package edu.alibaba.mpc4j.s2pc.pso.pid.gmr21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
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
        mpOprfReceiver.addLogLevel();
        mpOprfSender = OprfFactory.createMpOprfSender(clientRpc, serverParty, config.getMpOprfConfig());
        mpOprfSender.addLogLevel();
        psuClient = PsuFactory.createClient(clientRpc, serverParty, config.getPsuConfig());
        psuClient.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        mpOprfReceiver.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        mpOprfSender.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
        psuClient.setTaskId(taskIdPrf.getLong(2, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        mpOprfReceiver.setParallel(parallel);
        mpOprfSender.setParallel(parallel);
        psuClient.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        mpOprfReceiver.addLogLevel();
        mpOprfSender.addLogLevel();
        psuClient.addLogLevel();
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        mpOprfReceiver.init(maxClientElementSize);
        mpOprfSender.init(maxServerElementSize);
        psuClient.init(maxClientElementSize, maxServerElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public PidPartyOutput<T> pid(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int pidByteLength = PidUtils.getPidByteLength(otherSetSize, ownSetSize);
        pidMap = HashFactory.createInstance(envType, pidByteLength);
        clientElementByteArrays = ownElementArrayList.stream()
            .map(ObjectUtils::objectToByteArray)
            .toArray(byte[][]::new);
        // Alice and Bob invoke the OPRF functionality F_{oprf}.
        // Bob acts as receiver with input Y ′ and receives {F_{k_A}(y) | y ∈ Y'}.
        kaOprfOutput = mpOprfReceiver.oprf(clientElementByteArrays);
        // Alice and Bob invoke another OPRF functionality F_{oprf}.
        // Bob acts as sender and receives a PRF key k_B
        kbOprfKey = mpOprfSender.oprf(serverElementSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

        stopWatch.start();
        // Bob computes Pid(y_i)
        Map<ByteBuffer, T> clientPidMap = generateClientPidMap();
        stopWatch.stop();
        long pidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 2/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pidMapTime);

        stopWatch.start();
        // The parties invoke F_{psu}, with inputs {R_B(x) | y ∈ Y} for Bob
        Set<ByteBuffer> pidSet = psuClient.psu(clientPidMap.keySet(), serverElementSize, pidByteLength);
        stopWatch.stop();
        long psuTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 3/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), psuTime);

        stopWatch.start();
        // Bob sends union
        List<byte[]> unionPayload = pidSet.stream().map(ByteBuffer::array).collect(Collectors.toList());
        DataPacketHeader unionHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_UNION.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(unionHeader, unionPayload));
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 4/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), unionTime);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return new PidPartyOutput<>(pidByteLength, pidSet, clientPidMap);
    }

    private Map<ByteBuffer, T> generateClientPidMap() {
        IntStream clientElementIndexStream = IntStream.range(0, ownSetSize);
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
