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
import edu.alibaba.mpc4j.s2pc.pso.pmid.AbstractPmidServer;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidPartyOutput;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuServer;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.pso.oprf.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 多点ZCL22PMID协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/5/10
 */
public class Zcl22MpPmidServer<T> extends AbstractPmidServer<T> {
    /**
     * 多点OPRF发送方
     */
    private final MpOprfSender mpOprfSender;
    /**
     * 多点OPRF接收方
     */
    private final MpOprfReceiver mpOprfReceiver;
    /**
     * PSU服务端
     */
    private final PsuServer psuServer;
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
     * σ的OKVS密钥
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
     * k_A
     */
    private MpOprfSenderOutput mpOprfSenderOutput;
    /**
     * {F_{k_B}(x) | x ∈ X}
     */
    private MpOprfReceiverOutput mpOprfReceiverOutput;
    /**
     * k_x
     */
    private byte[][] kxArray;

    public Zcl22MpPmidServer(Rpc serverRpc, Party clientParty, Zcl22MpPmidConfig config) {
        super(Zcl22MpPmidPtoDesc.getInstance(), serverRpc, clientParty, config);
        mpOprfSender = OprfFactory.createMpOprfSender(serverRpc, clientParty, config.getMpOprfConfig());
        mpOprfSender.addLogLevel();
        mpOprfReceiver = OprfFactory.createMpOprfReceiver(serverRpc, clientParty, config.getMpOprfConfig());
        mpOprfReceiver.addLogLevel();
        psuServer = PsuFactory.createServer(serverRpc, clientParty, config.getPsuConfig());
        psuServer.addLogLevel();
        sigmaOkvsType = config.getSigmaOkvsType();
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
    public void init(int maxServerSetSize, int maxClientSetSize, int maxK) throws MpcAbortException {
        setInitInput(maxServerSetSize, maxClientSetSize, maxK);
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        mpOprfSender.init(maxClientSetSize);
        mpOprfReceiver.init(maxServerSetSize);
        psuServer.init(maxK * maxServerSetSize, maxK * maxClientSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        List<byte[]> keysPayload = new LinkedList<>();
        // PID映射密钥
        pmidMapPrfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(pmidMapPrfKey);
        keysPayload.add(pmidMapPrfKey);
        // σ映射密钥
        sigmaMapPrfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(sigmaMapPrfKey);
        keysPayload.add(sigmaMapPrfKey);
        // σ的OKVS密钥
        int okvsHashKeyNum = OkvsFactory.getHashNum(sigmaOkvsType);
        sigmaOkvsHashKeys = IntStream.range(0, okvsHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] okvsKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(okvsKey);
                keysPayload.add(okvsKey);
                return okvsKey;
            })
            .toArray(byte[][]::new);
        DataPacketHeader keysHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22MpPmidPtoDesc.PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keysHeader, keysPayload));
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyTime);

        initialized = true;
        info("{}{} Server Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public PmidPartyOutput<T> pmid(Set<T> serverElementSet, int clientSetSize, int k) throws MpcAbortException {
        setPtoInput(serverElementSet, clientSetSize, k);
        info("{}{} Server begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // PMID字节长度等于λ + log(nk) + log(m) = λ + log(m * n * k)
        int pmidByteLength = CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(
            LongUtils.ceilLog2((long)k * serverSetSize * clientSetSize)
        );
        pmidMapPrf = PrfFactory.createInstance(envType, pmidByteLength);
        pmidMapPrf.setKey(pmidMapPrfKey);
        // σ = λ + Max{log(nk), log(mk)}
        sigma = CommonConstants.STATS_BYTE_LENGTH + Math.max(
            LongUtils.ceilLog2((long) k * serverSetSize), LongUtils.ceilLog2((long) k * clientSetSize)
        );
        sigmaMapPrf = PrfFactory.createInstance(envType, sigma);
        sigmaMapPrf.setKey(sigmaMapPrfKey);
        // Alice and Bob invoke the OPRF functionality F_{oprf}.
        // Alice acts as sender and receives a PRF key k_A
        mpOprfSenderOutput = mpOprfSender.oprf(clientSetSize);
        stopWatch.stop();
        long mpOprfSenderTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 1/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), mpOprfSenderTime);

        stopWatch.start();
        // Alice and Bob invoke another OPRF functionality F_{oprf}.
        // Alice acts as receiver with input X and receives {F_{k_B}(x) | x ∈ X}.
        byte[][] xBytes = serverElementArrayList.stream()
            .map(ObjectUtils::objectToByteArray)
            .toArray(byte[][]::new);
        mpOprfReceiverOutput = mpOprfReceiver.oprf(xBytes);
        stopWatch.stop();
        long mpOprfReceiverTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 2/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), mpOprfReceiverTime);

        stopWatch.start();
        // Alice defines k_{x_i}
        generateKxArray();
        stopWatch.stop();
        long kxArrayTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 3/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), kxArrayTime);

        stopWatch.start();
        // Alice receives OKVS D from Bob.
        DataPacketHeader sigmaOkvsHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22MpPmidPtoDesc.PtoStep.CLIENT_SEND_SIGMA_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> sigmaOkvsPayload = rpc.receive(sigmaOkvsHeader).getPayload();
        // Alice computes id_{x_i}^(j)
        Map<ByteBuffer, T> serverPmidMap = handleSigmaOkvsPayload(sigmaOkvsPayload);
        stopWatch.stop();
        long pmidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 4/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pmidMapTime);

        stopWatch.start();
        // 双方同步对方PSU的元素数量
        List<byte[]> serverPsuSetSizePayload = new LinkedList<>();
        serverPsuSetSizePayload.add(IntUtils.intToByteArray(serverPmidMap.size()));
        DataPacketHeader serverPsuSetSizeHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22MpPmidPtoDesc.PtoStep.SERVER_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPsuSetSizeHeader, serverPsuSetSizePayload));

        DataPacketHeader clientPsuSetSizeHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22MpPmidPtoDesc.PtoStep.CLIENT_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientPsuSetSizePayload = rpc.receive(clientPsuSetSizeHeader).getPayload();
        MpcAbortPreconditions.checkArgument(clientPsuSetSizePayload.size() == 1);
        int clientPsuSetSize = IntUtils.byteArrayToInt(clientPsuSetSizePayload.remove(0));
        // Alice and Bob invoke the PSU functionality F_{psu}. Alice acts as sender with input ID_x.
        psuServer.psu(serverPmidMap.keySet(), clientPsuSetSize, pmidByteLength);
        stopWatch.stop();
        long psuTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 5/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), psuTime);

        stopWatch.start();
        // Alice receives union
        DataPacketHeader unionHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22MpPmidPtoDesc.PtoStep.CLIENT_SEND_UNION.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> unionPayload = rpc.receive(unionHeader).getPayload();
        MpcAbortPreconditions.checkArgument(unionPayload.size() >= serverSetSize);
        Set<ByteBuffer> pmidSet = unionPayload.stream()
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 6/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), unionTime);

        return new PmidPartyOutput<>(pmidByteLength, pmidSet, serverPmidMap);
    }

    private void generateKxArray() {
        // Alice defines k_{x_i} = H(F_{k_B}(xi) || K + 1) for i ∈ [m]
        IntStream serverElementIndexStream = IntStream.range(0, serverSetSize);
        serverElementIndexStream = parallel ? serverElementIndexStream.parallel() : serverElementIndexStream;
        kxArray = serverElementIndexStream
            .mapToObj(index -> {
                byte[] pid1 = mpOprfReceiverOutput.getPrf(index);
                return ByteBuffer.allocate(pid1.length + Integer.BYTES).put(pid1).putInt(k + 1).array();
            })
            .map(sigmaMapPrf::getBytes)
            .toArray(byte[][]::new);
    }

    private Map<ByteBuffer, T> handleSigmaOkvsPayload(List<byte[]> sigmaOkvsPayload) throws MpcAbortException {
        int sigmaOkvsM = OkvsFactory.getM(sigmaOkvsType, clientSetSize);
        MpcAbortPreconditions.checkArgument(sigmaOkvsPayload.size() == sigmaOkvsM);
        // 读取OKVS
        byte[][] sigmaOkvsStorage = sigmaOkvsPayload.toArray(new byte[0][]);
        Okvs<ByteBuffer> sigmaOkvs = OkvsFactory.createInstance(
            envType, sigmaOkvsType, clientSetSize, sigma * Byte.SIZE, sigmaOkvsHashKeys
        );
        // 初始化必要的参数
        BigInteger kBigInteger = BigInteger.valueOf(k);
        // 构建服务端PmidMap
        Map<ByteBuffer, T> serverPmidMap = new ConcurrentHashMap<>(serverSetSize * k);
        IntStream serverElementIndexStream = IntStream.range(0, serverSetSize);
        serverElementIndexStream = parallel ? serverElementIndexStream.parallel() : serverElementIndexStream;
        serverElementIndexStream.forEach(index -> {
            T x = serverElementArrayList.get(index);
            ByteBuffer sigmaX = ByteBuffer.wrap(sigmaMapPrf.getBytes(ObjectUtils.objectToByteArray(x)));
            byte[] pid0 = mpOprfSenderOutput.getPrf(ObjectUtils.objectToByteArray(x));
            byte[] pid1 = mpOprfReceiverOutput.getPrf(index);
            // Alice computes d_i = k_{x_i} ⊕ Decode_H(D, x_i) for i ∈ [m].
            byte[] kxBytes = kxArray[index];
            byte[] dxBytes = sigmaOkvs.decode(sigmaOkvsStorage, sigmaX);
            BytesUtils.xori(dxBytes, kxBytes);
            BigInteger dxBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(dxBytes);
            if (BigIntegerUtils.lessOrEqual(dxBigInteger, kBigInteger)
                && BigIntegerUtils.greater(dxBigInteger, BigInteger.ONE)
            ) {
                int dx = dxBigInteger.intValue();
                // If 1 < d_i ≤ K, Alice computes id(x_i^(j)) = H(F_{k_A}(x_i) || j) ⊕ H(F_{k_B(x_i) || j) for j ∈ [d_i];
                for (int j = 1; j <= dx; j++) {
                    byte[] extendPid0 = ByteBuffer.allocate(pid0.length + Integer.BYTES)
                        .put(pid0).putInt(j)
                        .array();
                    byte[] pmid0 = pmidMapPrf.getBytes(extendPid0);
                    byte[] extendPid1 = ByteBuffer.allocate(pid1.length + Integer.BYTES)
                        .put(pid1).putInt(j)
                        .array();
                    byte[] pmid1 = pmidMapPrf.getBytes(extendPid1);
                    BytesUtils.xori(pmid0, pmid1);
                    serverPmidMap.put(ByteBuffer.wrap(pmid0), x);
                }
            } else {
                // else, Alice computes id(x_i^(1)) = H(F_{k_A}(x_i) || 1) ⊕ H(F_{k_B(x_i) || 1);
                byte[] extendPid0 = ByteBuffer.allocate(pid0.length + Integer.BYTES)
                    .put(pid0).putInt(1)
                    .array();
                byte[] pmid0 = pmidMapPrf.getBytes(extendPid0);
                byte[] extendPid1 = ByteBuffer.allocate(pid1.length + Integer.BYTES)
                    .put(pid1).putInt(1)
                    .array();
                byte[] pmid1 = pmidMapPrf.getBytes(extendPid1);
                BytesUtils.xori(pmid0, pmid1);
                serverPmidMap.put(ByteBuffer.wrap(pmid0), x);
            }
        });
        kxArray = null;
        mpOprfSenderOutput = null;
        mpOprfReceiverOutput = null;
        return serverPmidMap;
    }
}
