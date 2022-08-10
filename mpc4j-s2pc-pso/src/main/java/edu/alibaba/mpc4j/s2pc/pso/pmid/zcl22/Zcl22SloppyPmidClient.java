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
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.Okvs;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.s2pc.pso.pmid.AbstractPmidClient;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidPartyOutput;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.pso.oprf.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Weiran Liu
 * @date 2022/5/14
 */
public class Zcl22SloppyPmidClient<T> extends AbstractPmidClient<T> {
    /**
     * OPRF发送方
     */
    private final OprfSender oprfSender;
    /**
     * OPRF接收方
     */
    private final OprfReceiver oprfReceiver;
    /**
     * PSU协议客户端
     */
    private final PsuClient psuClient;
    /**
     * Sloppy的OKVS类型
     */
    private final OkvsType sloppyOkvsType;
    /**
     * σ的OKVS类型
     */
    private final OkvsType sigmaOkvsType;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * 布谷鸟哈希函数数量
     */
    private final int cuckooHashNum;
    /**
     * PID映射密钥
     */
    private byte[] pidMapPrfKey;
    /**
     * 客户端PID映射密钥
     */
    private byte[] clientPidPrfKey;
    /**
     * PMID映射密钥
     */
    private byte[] pmidMapPrfKey;
    /**
     * σ映射密钥
     */
    private byte[] sigmaMapPrfKey;
    /**
     * 客户端PID的OKVS密钥
     */
    private byte[][] clientOkvsHashKeys;
    /**
     * 服务端PID的OKVS密钥
     */
    private byte[][] serverOkvsHashKeys;
    /**
     * σ的OKVS密钥
     */
    private byte[][] sigmaOkvsHashKeys;
    /**
     * PID字节长度
     */
    private int pidByteLength;
    /**
     * PID映射函数
     */
    private Prf pidMapPrf;
    /**
     * 客户端PID伪随机函数
     */
    private Prf clientPidPrf;
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
     * 服务端桶数量
     */
    private int serverBinNum;
    /**
     * 服务端布谷鸟哈希
     */
    private Prf[] serverCuckooHashes;
    /**
     * (k_1^B, ..., k_m^B)
     */
    private OprfSenderOutput oprfSenderOutput;
    /**
     * 客户端桶数量
     */
    private int clientBinNum;
    /**
     * 客户端无贮存区布谷鸟哈希
     */
    private CuckooHashBin<T> clientCuckooHashBin;
    /**
     * (f_1^B, ..., f_m^B)
     */
    private OprfReceiverOutput oprfReceiverOutput;
    /**
     * 服务端PID映射
     */
    private Map<ByteBuffer, T> clientPidMap;
    /**
     * k_y
     */
    private  Map<ByteBuffer, byte[]> kyMap;

    public Zcl22SloppyPmidClient(Rpc clientRpc, Party serverParty, Zcl22SloppyPmidConfig config) {
        super(Zcl22SloppyPmidPtoDesc.getInstance(), clientRpc, serverParty, config);
        oprfSender = OprfFactory.createOprfSender(clientRpc, serverParty, config.getOprfConfig());
        oprfSender.addLogLevel();
        oprfReceiver = OprfFactory.createOprfReceiver(clientRpc, serverParty, config.getOprfConfig());
        oprfReceiver.addLogLevel();
        psuClient = PsuFactory.createClient(clientRpc, serverParty, config.getPsuConfig());
        psuClient.addLogLevel();
        sloppyOkvsType = config.getSloppyOkvsType();
        sigmaOkvsType = config.getSigmaOkvsType();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        oprfSender.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        oprfReceiver.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
        psuClient.setTaskId(taskIdPrf.getLong(2, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        oprfSender.setParallel(parallel);
        oprfReceiver.setParallel(parallel);
        psuClient.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        oprfSender.addLogLevel();
        oprfReceiver.addLogLevel();
        psuClient.addLogLevel();
    }

    @Override
    public void init(int maxClientSetSize, int maxServerSetSize, int maxK) throws MpcAbortException {
        setInitInput(maxClientSetSize, maxServerSetSize, maxK);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int maxServerBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerSetSize);
        oprfSender.init(maxServerBinNum);
        int maxClientBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientSetSize);
        oprfReceiver.init(maxClientBinNum);
        psuClient.init(maxK * maxClientSetSize, maxK * maxServerSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        List<byte[]> clientKeysPayload = new LinkedList<>();
        // 初始化s^B
        clientPidPrfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(clientPidPrfKey);
        // 客户端PID的OKVS密钥
        int sloppyOkvsHashKeyNum = OkvsFactory.getHashNum(sloppyOkvsType);
        clientOkvsHashKeys = IntStream.range(0, sloppyOkvsHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] okvsKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(okvsKey);
                clientKeysPayload.add(okvsKey);
                return okvsKey;
            })
            .toArray(byte[][]::new);
        DataPacketHeader clientKeysHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22SloppyPmidPtoDesc.PtoStep.CLIENT_SEND_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientKeysHeader, clientKeysPayload));
        // 接收服务端密钥
        DataPacketHeader serverKeysHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22SloppyPmidPtoDesc.PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverKeysPayload = rpc.receive(serverKeysHeader).getPayload();
        // PID映射密钥、PMID映射密钥、σ映射密钥、服务端PID的OKVS密钥、σ的OKVS密钥
        MpcAbortPreconditions.checkArgument(
            serverKeysPayload.size() == 3 + sloppyOkvsHashKeyNum + OkvsFactory.getHashNum(sigmaOkvsType)
        );
        pidMapPrfKey = serverKeysPayload.remove(0);
        pmidMapPrfKey = serverKeysPayload.remove(0);
        sigmaMapPrfKey = serverKeysPayload.remove(0);
        serverOkvsHashKeys = IntStream.range(0, sloppyOkvsHashKeyNum)
            .mapToObj(hashIndex -> serverKeysPayload.remove(0))
            .toArray(byte[][]::new);
        sigmaOkvsHashKeys = serverKeysPayload.toArray(new byte[0][]);
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
        // PID字节长度等于λ + log(n) + log(m) = λ + log(m * n)
        pidByteLength = CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(
            LongUtils.ceilLog2((long) clientSetSize * serverSetSize)
        );
        pidMapPrf = PrfFactory.createInstance(envType, pidByteLength);
        pidMapPrf.setKey(pidMapPrfKey);
        clientPidPrf = PrfFactory.createInstance(envType, pidByteLength);
        clientPidPrf.setKey(clientPidPrfKey);
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
        // Alice inserts items into cuckoo hash
        DataPacketHeader serverCuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22SloppyPmidPtoDesc.PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverCuckooHashKeyPayload = rpc.receive(serverCuckooHashKeyHeader).getPayload();
        handleServerCuckooHashKeyPayload(serverCuckooHashKeyPayload);
        stopWatch.stop();
        long serverCuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), serverCuckooHashTime);

        stopWatch.start();
        // The parties call F_{bOPRF}, where Bob is sender.
        // Bob receives output (k_1^B, ..., k_m^B)
        oprfSenderOutput = oprfSender.oprf(serverBinNum);
        stopWatch.stop();
        long serverOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), serverOprfTime);

        stopWatch.start();
        // Bob sends PID OKVS
        List<byte[]> clientPidOkvsPayload = generateClientPidOkvsPayload();
        DataPacketHeader clientPidOkvsHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22SloppyPmidPtoDesc.PtoStep.CLIENT_SEND_PID_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPidOkvsHeader, clientPidOkvsPayload));
        stopWatch.stop();
        long clientPidOkvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 3/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), clientPidOkvsTime);

        stopWatch.start();
        // Bob insert items into cuckoo hash
        List<byte[]> clientCuckooHashKeyPayload = generateClientCuckooHashKeyPayload();
        DataPacketHeader clientCuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22SloppyPmidPtoDesc.PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientCuckooHashKeyHeader, clientCuckooHashKeyPayload));
        stopWatch.stop();
        long clientCuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 4/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), clientCuckooHashTime);

        stopWatch.start();
        // The parties call F_{bOPRF}, where Bob is receiver with input B.
        // Bob receives output (f_1^B, ..., f_m^B), where f_j^B = PRF(k_j^A, x||i)
        byte[][] clientOprfInputs = IntStream.range(0, clientBinNum)
            .mapToObj(clientBinIndex -> {
                HashBinEntry<T> hashBinEntry = clientCuckooHashBin.getHashBinEntry(clientBinIndex);
                byte[] elementBytes = hashBinEntry.getItemByteArray();
                return ByteBuffer.allocate(elementBytes.length + Integer.BYTES)
                    .put(elementBytes)
                    .putInt(hashBinEntry.getHashIndex())
                    .array();
            })
            .toArray(byte[][]::new);
        oprfReceiverOutput = oprfReceiver.oprf(clientOprfInputs);
        stopWatch.stop();
        long clientOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 5/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), clientOprfTime);

        stopWatch.start();
        // Bob receives PID OKVS
        DataPacketHeader serverPidOkvsHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22SloppyPmidPtoDesc.PtoStep.SERVER_SEND_PID_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverOkvsPayload = rpc.receive(serverPidOkvsHeader).getPayload();
        clientPidMap = handleServerPidOkvsPayload(serverOkvsPayload);
        stopWatch.stop();
        long serverOkvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 6/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), serverOkvsTime);

        stopWatch.start();
        // Bob defines k_{y_i}
        generateKyMap();
        stopWatch.stop();
        long kyArrayTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 7/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), kyArrayTime);

        stopWatch.start();
        // Bob computes an SIGMA OKVS D
        List<byte[]> sigmaOkvsPayload = generateSigmaOkvsPayload();
        DataPacketHeader sigmaOkvsHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22SloppyPmidPtoDesc.PtoStep.CLIENT_SEND_SIGMA_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sigmaOkvsHeader, sigmaOkvsPayload));
        // Bob computes idy_i^(j)
        Map<ByteBuffer, T> clientPmidMap = generateClientPmidMap();
        stopWatch.stop();
        long pmidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 8/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pmidMapTime);

        stopWatch.start();
        // 双方同步对方PSU的元素数量
        List<byte[]> clientPsuSetSizePayload = new LinkedList<>();
        clientPsuSetSizePayload.add(IntUtils.intToByteArray(clientPmidMap.size()));
        DataPacketHeader clientPsuSetSizeHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22SloppyPmidPtoDesc.PtoStep.CLIENT_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPsuSetSizeHeader, clientPsuSetSizePayload));

        DataPacketHeader serverPsuSetSizeHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22SloppyPmidPtoDesc.PtoStep.SERVER_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
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
        info("{}{} Client Step 9/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), psuTime);

        stopWatch.start();
        // Bob sends union
        List<byte[]> unionPayload = pmidSet.stream().map(ByteBuffer::array).collect(Collectors.toList());
        DataPacketHeader unionHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22SloppyPmidPtoDesc.PtoStep.CLIENT_SEND_UNION.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(unionHeader, unionPayload));
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 10/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), unionTime);

        return new PmidPartyOutput<>(pmidByteLength, pmidSet, clientPmidMap);
    }

    private void handleServerCuckooHashKeyPayload(List<byte[]> serverCuckooHashKeyPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(serverCuckooHashKeyPayload.size() == cuckooHashNum);
        serverBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverSetSize);
        serverCuckooHashes = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                byte[] key = serverCuckooHashKeyPayload.remove(0);
                Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
                hash.setKey(key);
                return hash;
            })
            .toArray(Prf[]::new);
    }

    private List<byte[]> generateClientPidOkvsPayload() {
        // 客户端字节元素
        ByteBuffer[] clientElementByteBuffers = clientElementArrayList.stream()
            .map(ObjectUtils::objectToByteArray)
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        // 客户端扩展字节元素
        ByteBuffer[][] clientExtendElementByteBuffers = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> Arrays.stream(clientElementByteBuffers)
                .map(elementByteBuffer -> {
                    byte[] elementBytes = elementByteBuffer.array();
                    return ByteBuffer.allocate(elementBytes.length + Integer.BYTES)
                        .put(elementBytes)
                        .putInt(hashIndex)
                        .array();
                })
                .map(ByteBuffer::wrap)
                .toArray(ByteBuffer[]::new))
            .toArray(ByteBuffer[][]::new);
        // key
        ByteBuffer[] clientPidOkvsKeyArray = Arrays.stream(clientExtendElementByteBuffers)
            .map(hashExtendElementByteBuffers -> {
                Stream<ByteBuffer> hashExtendElementStream = Arrays.stream(hashExtendElementByteBuffers);
                hashExtendElementStream = parallel ? hashExtendElementStream.parallel() : hashExtendElementStream;
                return hashExtendElementStream
                    .map(ByteBuffer::array)
                    .map(pidMapPrf::getBytes)
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        // value
        byte[][] clientPidOkvsValueArray = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                // value值涉及密码学操作，并发处理
                IntStream clientElementIntStream = IntStream.range(0, clientSetSize);
                clientElementIntStream = parallel ? clientElementIntStream.parallel() : clientElementIntStream;
                return clientElementIntStream
                    .mapToObj(index -> {
                        byte[] elementBytes = clientElementByteBuffers[index].array();
                        byte[] extendElementBytes = clientExtendElementByteBuffers[hashIndex][index].array();
                        byte[] pid0 = clientPidPrf.getBytes(elementBytes);
                        int serverBinIndex = serverCuckooHashes[hashIndex].getInteger(elementBytes, serverBinNum);
                        byte[] pid1 = pidMapPrf.getBytes(oprfSenderOutput.getPrf(serverBinIndex, extendElementBytes));
                        BytesUtils.xori(pid0, pid1);
                        return pid0;
                    })
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
        Map<ByteBuffer, byte[]> clientPidOkvsKeyValueMap = IntStream.range(0, clientSetSize * cuckooHashNum)
            .boxed()
            .collect(Collectors.toMap(index -> clientPidOkvsKeyArray[index], index -> clientPidOkvsValueArray[index]));
        Okvs<ByteBuffer> clientPidOkvs = OkvsFactory.createInstance(
            envType, sloppyOkvsType, clientSetSize * cuckooHashNum, pidByteLength * Byte.SIZE, clientOkvsHashKeys
        );
        // 编码可以并行处理
        clientPidOkvs.setParallelEncode(parallel);
        byte[][] clientPidOkvsStorage = clientPidOkvs.encode(clientPidOkvsKeyValueMap);
        oprfSenderOutput = null;
        return Arrays.stream(clientPidOkvsStorage).collect(Collectors.toList());
    }

    private List<byte[]> generateClientCuckooHashKeyPayload() {
        clientBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientSetSize);
        // 设置布谷鸟哈希，如果发现不能构造成功，则可以重复构造
        boolean success = false;
        byte[][] clientCuckooHashKeys = null;
        while (!success) {
            try {
                clientCuckooHashKeys = IntStream.range(0, cuckooHashNum)
                    .mapToObj(hashIndex -> {
                        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                        secureRandom.nextBytes(key);
                        return key;
                    })
                    .toArray(byte[][]::new);
                clientCuckooHashBin = CuckooHashBinFactory.createCuckooHashBin(
                    envType, cuckooHashBinType, clientSetSize, clientCuckooHashKeys
                );
                // 将客户端消息插入到CuckooHash中
                clientCuckooHashBin.insertItems(clientElementArrayList);
                if (clientCuckooHashBin.itemNumInStash() == 0) {
                    success = true;
                }
            } catch (ArithmeticException ignored) {
                // 如果插入不成功，就重新插入
            }
        }
        // 如果成功，则向布谷鸟哈希的空余位置插入随机元素
        clientCuckooHashBin.insertPaddingItems(secureRandom);
        return Arrays.stream(clientCuckooHashKeys).collect(Collectors.toList());
    }

    private Map<ByteBuffer, T> handleServerPidOkvsPayload(List<byte[]> serverPidOkvsPayload) throws MpcAbortException {
        int serverOkvsM = OkvsFactory.getM(sloppyOkvsType, serverSetSize * cuckooHashNum);
        MpcAbortPreconditions.checkArgument(serverPidOkvsPayload.size() == serverOkvsM);
        byte[][] serverOkvsStorage = serverPidOkvsPayload.toArray(new byte[0][]);
        Okvs<ByteBuffer> serverOkvs = OkvsFactory.createInstance(
            envType, sloppyOkvsType, serverSetSize * cuckooHashNum, pidByteLength * Byte.SIZE, serverOkvsHashKeys
        );
        IntStream clientBinIndexStream = IntStream.range(0, clientBinNum);
        clientBinIndexStream = parallel ? clientBinIndexStream.parallel() : clientBinIndexStream;
        ByteBuffer[] clientPids = clientBinIndexStream
            .mapToObj(clientBinIndex -> {
                HashBinEntry<T> hashBinEntry = clientCuckooHashBin.getHashBinEntry(clientBinIndex);
                int hashIndex = hashBinEntry.getHashIndex();
                // 虚拟元素不包含PID
                if (hashIndex == HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                    return null;
                }
                // 非虚拟元素，拼接字符串
                byte[] elementBytes = hashBinEntry.getItemByteArray();
                byte[] extendElementBytes = ByteBuffer.allocate(elementBytes.length + Integer.BYTES)
                    .put(elementBytes)
                    .putInt(hashIndex)
                    .array();
                ByteBuffer pidExtendElementBytes = ByteBuffer.wrap(pidMapPrf.getBytes(extendElementBytes));
                // R^B(y) = P^A(y || i) ⊕ f^B_{h_i(y)} ⊕ PRF'(s^B, y)
                byte[] pidBytes = serverOkvs.decode(serverOkvsStorage, pidExtendElementBytes);
                BytesUtils.xori(pidBytes, pidMapPrf.getBytes(oprfReceiverOutput.getPrf(clientBinIndex)));
                BytesUtils.xori(pidBytes, clientPidPrf.getBytes(elementBytes));
                return ByteBuffer.wrap(pidBytes);
            })
            .toArray(ByteBuffer[]::new);
        Map<ByteBuffer, T> clientPidMap = new HashMap<>(clientBinNum);
        IntStream.range(0, clientBinNum).forEach(clientBinIndex -> {
            if (clientPids[clientBinIndex] != null) {
                clientPidMap.put(clientPids[clientBinIndex], clientCuckooHashBin.getHashBinEntry(clientBinIndex).getItem());
            }
        });
        clientCuckooHashBin = null;
        oprfReceiverOutput = null;
        return clientPidMap;
    }

    private void generateKyMap() {
        // Alice defines k_{x_i} = H(F_{k_B}(xi) || K + 1) for i ∈ [m]
        Stream<ByteBuffer> clientPidStream = clientPidMap.keySet().stream();
        clientPidStream = parallel ? clientPidStream.parallel() : clientPidStream;
        kyMap = clientPidStream
            .collect(Collectors.toMap(
                clientPid -> clientPid,
                clientPid -> {
                    byte[] pid = clientPid.array();
                    byte[] extendPid = ByteBuffer.allocate(pid.length + Integer.BYTES).put(pid).putInt(k + 1).array();
                    return sigmaMapPrf.getBytes(extendPid);
                })
            );
    }

    private List<byte[]> generateSigmaOkvsPayload() {
        // For each pid
        Stream<ByteBuffer> clientPidStream = clientPidMap.keySet().stream();
        clientPidStream = parallel ? clientPidStream.parallel() : clientPidStream;
        Map<ByteBuffer, byte[]> sigmaKeyValueMap = clientPidStream
            .collect(Collectors.toMap(
                clientPid -> {
                    T y = clientPidMap.get(clientPid);
                    return ByteBuffer.wrap(sigmaMapPrf.getBytes(ObjectUtils.objectToByteArray(y)));
                },
                clientPid -> {
                    T y = clientPidMap.get(clientPid);
                    int ky = clientElementMap.get(y);
                    byte[] dy;
                    if (ky == 1) {
                        // if k_j = 1, Bob selects a random c_j ← {0, 1}^σ
                        dy = new byte[sigma];
                        secureRandom.nextBytes(dy);
                    } else {
                        // else defines c_j = k_j
                        dy = IntUtils.nonNegIntToFixedByteArray(ky, sigma);
                    }
                    BytesUtils.xori(dy, kyMap.get(clientPid));
                    return dy;
                }
            ));
        Okvs<ByteBuffer> sigmaOkvs = OkvsFactory.createInstance(
            envType, sigmaOkvsType, clientSetSize, sigma * Byte.SIZE, sigmaOkvsHashKeys
        );
        // OKVS编码可以并行处理
        sigmaOkvs.setParallelEncode(parallel);
        byte[][] sigmaOkvsStorage = sigmaOkvs.encode(sigmaKeyValueMap);
        kyMap = null;
        return Arrays.stream(sigmaOkvsStorage).collect(Collectors.toList());
    }

    private Map<ByteBuffer, T> generateClientPmidMap() {
        // 构建客户端PmidMap
        Map<ByteBuffer, T> clientPmidMap = new ConcurrentHashMap<>(clientSetSize * k);
        Stream<ByteBuffer> clientPidStream = clientPidMap.keySet().stream();
        clientPidStream = parallel ? clientPidStream.parallel() : clientPidStream;
        clientPidStream.forEach(clientPid -> {
            T y = clientPidMap.get(clientPid);
            byte[] clientPidBytes = clientPid.array();
            for (int j = 1; j <= clientElementMap.get(y); j++) {
                byte[] extendClientPid = ByteBuffer.allocate(clientPidBytes.length + Integer.BYTES)
                    .put(clientPidBytes).put(IntUtils.intToByteArray(j))
                    .array();
                byte[] pmid = pmidMapPrf.getBytes(extendClientPid);
                clientPmidMap.put(ByteBuffer.wrap(pmid), y);
            }
        });
        return clientPmidMap;
    }
}
