package edu.alibaba.mpc4j.s2pc.pso.pmid.zcl22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.okve.okvs.Okvs;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.s2pc.pso.oprf.*;
import edu.alibaba.mpc4j.s2pc.pso.pmid.AbstractPmidClient;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidPartyOutput;
import edu.alibaba.mpc4j.s2pc.pso.pmid.zcl22.Zcl22MpPmidPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 多点ZCL22PMID协议客户端
 *
 * @author Weiran Liu
 * @date 2022/5/10
 */
public class Zcl22MpPmidClient<T> extends AbstractPmidClient<T> {
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
     * σ的OKVS类型
     */
    private final OkvsType sigmaOkvsType;
    /**
     * PMID映射密钥
     */
    private byte[] pmidMapPrfKey;
    /**
     * σ映射密钥
     */
    private byte[] sigmaMapPrfKey;
    /**
     * OKVS密钥
     */
    private byte[][] sigmaOkvsHashKeys;
    /**
     * PMID映射函数
     */
    private Prf pmidMapPrf;
    /**
     * σ字节长度
     */
    private int sigma;
    /**
     * σ映射函数
     */
    private Prf sigmaMapPrf;
    /**
     * {F_{k_A}(y) | y ∈ Y'å}
     */
    private MpOprfReceiverOutput mpOprfReceiverOutput;
    /**
     * k_B
     */
    private MpOprfSenderOutput mpOprfSenderOutput;
    /**
     * PID1数组
     */
    private byte[][] pid1Array;
    /**
     * k_y
     */
    private byte[][] kyArray;

    public Zcl22MpPmidClient(Rpc serverRpc, Party clientParty, Zcl22MpPmidConfig config) {
        super(Zcl22MpPmidPtoDesc.getInstance(), serverRpc, clientParty, config);
        mpOprfReceiver = OprfFactory.createMpOprfReceiver(serverRpc, clientParty, config.getMpOprfConfig());
        mpOprfReceiver.addLogLevel();
        mpOprfSender = OprfFactory.createMpOprfSender(serverRpc, clientParty, config.getMpOprfConfig());
        mpOprfSender.addLogLevel();
        psuClient = PsuFactory.createClient(serverRpc, clientParty, config.getPsuConfig());
        psuClient.addLogLevel();
        sigmaOkvsType = config.getSigmaOkvsType();
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
        mpOprfSender.setParallel(parallel);
        mpOprfReceiver.setParallel(parallel);
        psuClient.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        mpOprfSender.addLogLevel();
        mpOprfReceiver.addLogLevel();
        psuClient.addLogLevel();
    }

    @Override
    public void init(int maxClientSetSize, int maxServerSetSize, int maxK) throws MpcAbortException {
        setInitInput(maxClientSetSize, maxServerSetSize, maxK);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        mpOprfReceiver.init(maxClientSetSize);
        mpOprfSender.init(maxServerSetSize);
        psuClient.init(maxK * maxClientSetSize, maxK * maxServerSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        DataPacketHeader keysHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> keysPayload = rpc.receive(keysHeader).getPayload();
        // PID映射密钥、σ映射密钥、σ的OKVS密钥
        MpcAbortPreconditions.checkArgument(keysPayload.size() == 2 + OkvsFactory.getHashNum(sigmaOkvsType));
        pmidMapPrfKey = keysPayload.remove(0);
        sigmaMapPrfKey = keysPayload.remove(0);
        sigmaOkvsHashKeys = keysPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyTime);

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public PmidPartyOutput<T> pmid(Map<T, Integer> clientElementMap, int serverSetSize) throws MpcAbortException {
        setPtoInput(clientElementMap, serverSetSize);
        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // PMID字节长度等于λ + log(nk) + log(m) = λ + log(m * n * k)
        int pmidByteLength = CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(
            LongUtils.ceilLog2((long)k * clientSetSize * serverSetSize)
        );
        pmidMapPrf = PrfFactory.createInstance(envType, pmidByteLength);
        pmidMapPrf.setKey(pmidMapPrfKey);
        // σ = λ + Max{log(nk), log(mk)}
        sigma = CommonConstants.STATS_BYTE_LENGTH + Math.max(
            LongUtils.ceilLog2((long) k * clientSetSize), LongUtils.ceilLog2((long) k * serverSetSize)
        );
        sigmaMapPrf = PrfFactory.createInstance(envType, sigma);
        sigmaMapPrf.setKey(sigmaMapPrfKey);
        // Alice and Bob invoke the OPRF functionality F_{oprf}.
        // Bob acts as receiver with input Y ′ and receives {F_{k_A}(y) | y ∈ Y'}.
        byte[][] yBytes = clientElementArrayList.stream()
            .map(ObjectUtils::objectToByteArray)
            .toArray(byte[][]::new);
        mpOprfReceiverOutput = mpOprfReceiver.oprf(yBytes);
        stopWatch.stop();
        long mpOprfReceiverTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), mpOprfReceiverTime);

        stopWatch.start();
        // Alice and Bob invoke another OPRF functionality F_{oprf}.
        // Bob acts as sender and receives a PRF key k_B
        mpOprfSenderOutput = mpOprfSender.oprf(serverSetSize);
        stopWatch.stop();
        long mpOprfSenderTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), mpOprfSenderTime);

        stopWatch.start();
        // Bob defines k_{y_i}
        generatePid1Array();
        generateKyArray();
        stopWatch.stop();
        long kyArrayTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 3/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), kyArrayTime);

        stopWatch.start();
        // Bob computes an OKVS D
        List<byte[]> sigmaOkvsPayload = generateSigmaOkvsPayload();
        DataPacketHeader sigmaOkvsHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_SIGMA_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sigmaOkvsHeader, sigmaOkvsPayload));
        // Bob computes id_{y_i}^(j)
        Map<ByteBuffer, T> clientPmidMap = generateClientPmidMap();
        stopWatch.stop();
        long pmidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 4/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pmidMapTime);

        stopWatch.start();
        // 双方同步对方PSU的元素数量
        List<byte[]> clientPsuSetSizePayload = new LinkedList<>();
        clientPsuSetSizePayload.add(IntUtils.intToByteArray(clientPmidMap.size()));
        DataPacketHeader clientPsuSetSizeHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPsuSetSizeHeader, clientPsuSetSizePayload));

        DataPacketHeader serverPsuSetSizeHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPsuSetSizePayload = rpc.receive(serverPsuSetSizeHeader).getPayload();
        MpcAbortPreconditions.checkArgument(serverPsuSetSizePayload.size() == 1);
        int serverPsuSetSize = IntUtils.byteArrayToInt(serverPsuSetSizePayload.remove(0));
        // Alice and Bob invoke the PSU functionality F_{psu}. Bob acts as receiver with input ID_y and receives the union
        Set<ByteBuffer> pmidSet = psuClient.psu(clientPmidMap.keySet(), serverPsuSetSize, pmidByteLength);
        stopWatch.stop();
        long psuTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 5/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), psuTime);

        stopWatch.start();
        // Bob sends union
        List<byte[]> unionPayload = pmidSet.stream().map(ByteBuffer::array).collect(Collectors.toList());
        DataPacketHeader unionHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_UNION.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(unionHeader, unionPayload));
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 6/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), unionTime);

        return new PmidPartyOutput<>(pmidByteLength, pmidSet, clientPmidMap);
    }

    private void generatePid1Array() {
        IntStream clientElementIndexStream = IntStream.range(0, clientSetSize);
        clientElementIndexStream = parallel ? clientElementIndexStream.parallel() : clientElementIndexStream;
        pid1Array = clientElementIndexStream
            .mapToObj(index -> {
            T y = clientElementArrayList.get(index);
            return mpOprfSenderOutput.getPrf(ObjectUtils.objectToByteArray(y));
            })
            .toArray(byte[][]::new);
        mpOprfSenderOutput = null;
    }

    private void generateKyArray() {
        // Alice defines k_{x_i} = H(F_{k_B}(xi) || K + 1) for i ∈ [m]
        IntStream clientElementIndexStream = IntStream.range(0, clientSetSize);
        clientElementIndexStream = parallel ? clientElementIndexStream.parallel() : clientElementIndexStream;
        kyArray = clientElementIndexStream
            .mapToObj(index -> {
                byte[] pid1 = pid1Array[index];
                return ByteBuffer.allocate(pid1.length + Integer.BYTES).put(pid1).putInt(k + 1).array();
            })
            .map(sigmaMapPrf::getBytes)
            .toArray(byte[][]::new);
    }

    private List<byte[]> generateSigmaOkvsPayload() {
        // For j ∈ [n]
        IntStream clientElementIndexStream = IntStream.range(0, clientSetSize);
        clientElementIndexStream = parallel ? clientElementIndexStream.parallel() : clientElementIndexStream;
        Map<ByteBuffer, byte[]> sigmaKeyValueMap = clientElementIndexStream
            .boxed()
            .collect(Collectors.toMap(
                index -> {
                    T y = clientElementArrayList.get(index);
                    return ByteBuffer.wrap(sigmaMapPrf.getBytes(ObjectUtils.objectToByteArray(y)));
                },
                index -> {
                    int ky = clientElementMap.get(clientElementArrayList.get(index));
                    byte[] dy;
                    if (ky == 1) {
                        // if k_j = 1, Bob selects a random c_j ← {0, 1}^σ
                        dy = new byte[sigma];
                        secureRandom.nextBytes(dy);
                    } else {
                        // else defines c_j = k_j
                        dy = IntUtils.nonNegIntToFixedByteArray(ky, sigma);
                    }
                    BytesUtils.xori(dy, kyArray[index]);
                    return dy;
                }
            ));
        Okvs<ByteBuffer> sigmaOkvs = OkvsFactory.createInstance(
            envType, sigmaOkvsType, clientSetSize, sigma * Byte.SIZE, sigmaOkvsHashKeys
        );
        // OKVS编码可以并行处理
        sigmaOkvs.setParallelEncode(parallel);
        byte[][] sigmaOkvsStorage = sigmaOkvs.encode(sigmaKeyValueMap);
        kyArray = null;
        return Arrays.stream(sigmaOkvsStorage).collect(Collectors.toList());
    }

    private Map<ByteBuffer, T> generateClientPmidMap() {
        // 构建客户端PmidMap
        Map<ByteBuffer, T> clientPmidMap = new ConcurrentHashMap<>(clientSetSize * k);
        IntStream clientElementIndexStream = IntStream.range(0, clientSetSize);
        clientElementIndexStream = parallel ? clientElementIndexStream.parallel() : clientElementIndexStream;
        clientElementIndexStream.forEach(index -> {
            T y = clientElementArrayList.get(index);
            byte[] pid0 = mpOprfReceiverOutput.getPrf(index);
            byte[] pid1 = pid1Array[index];
            for (int j = 1; j <= clientElementMap.get(y); j++) {
                byte[] extendPid0 = ByteBuffer.allocate(pid0.length + Integer.BYTES)
                    .put(pid0).put(IntUtils.intToByteArray(j))
                    .array();
                byte[] pmid0 = pmidMapPrf.getBytes(extendPid0);
                byte[] extendPid1 = ByteBuffer.allocate(pid1.length + Integer.BYTES)
                    .put(pid1).put(IntUtils.intToByteArray(j))
                    .array();
                byte[] pmid1 = pmidMapPrf.getBytes(extendPid1);
                BytesUtils.xori(pmid0, pmid1);
                clientPmidMap.put(ByteBuffer.wrap(pmid0), y);
            }
        });
        pid1Array = null;
        mpOprfReceiverOutput = null;
        return clientPmidMap;
    }
}
