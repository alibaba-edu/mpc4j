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
import edu.alibaba.mpc4j.s2pc.pso.oprf.*;
import edu.alibaba.mpc4j.s2pc.pso.pmid.AbstractPmidServer;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidPartyOutput;
import edu.alibaba.mpc4j.s2pc.pso.pmid.zcl22.Zcl22SloppyPmidPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuServer;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * ZCL22宽松PMID协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/5/14
 */
public class Zcl22SloppyPmidServer<T> extends AbstractPmidServer<T> {
    /**
     * OPRF接收方
     */
    private final OprfReceiver oprfReceiver;
    /**
     * OPRF发送方
     */
    private final OprfSender oprfSender;
    /**
     * PSU协议服务端
     */
    private final PsuServer psuServer;
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
     * 服务端PID映射密钥
     */
    private byte[] serverPidPrfKey;
    /**
     * PMID映射密钥
     */
    private byte[] pmidMapPrfKey;
    /**
     * σ密钥
     */
    private byte[] sigmaMapPrfKey;
    /**
     * 服务端PID的OKVS密钥
     */
    private byte[][] serverPidOkvsHashKeys;
    /**
     * 客户端PID的OKVS密钥
     */
    private byte[][] clientPidOkvsHashKeys;
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
     * 服务端PID伪随机函数
     */
    private Prf serverPidPrf;
    /**
     * PMID映射伪随机函数
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
     * 服务端无贮存区布谷鸟哈希
     */
    private CuckooHashBin<T> serverCuckooHashBin;
    /**
     * (f_1^A, ..., f_m^A)
     */
    private OprfReceiverOutput oprfReceiverOutput;
    /**
     * 客户端桶数量
     */
    private int clientBinNum;
    /**
     * 客户端布谷鸟哈希
     */
    private Prf[] clientCuckooHashes;
    /**
     * (k_1^A, ..., k_m^A)
     */
    private OprfSenderOutput oprfSenderOutput;
    /**
     * 服务端PID映射
     */
    private Map<ByteBuffer, T> serverPidMap;
    /**
     * k_x
     */
    private Map<ByteBuffer, byte[]> kxMap;

    public Zcl22SloppyPmidServer(Rpc serverRpc, Party clientParty, Zcl22SloppyPmidConfig config) {
        super(Zcl22SloppyPmidPtoDesc.getInstance(), serverRpc, clientParty, config);
        oprfReceiver = OprfFactory.createOprfReceiver(serverRpc, clientParty, config.getOprfConfig());
        oprfReceiver.addLogLevel();
        oprfSender = OprfFactory.createOprfSender(serverRpc, clientParty, config.getOprfConfig());
        oprfSender.addLogLevel();
        psuServer = PsuFactory.createServer(serverRpc, clientParty, config.getPsuConfig());
        psuServer.addLogLevel();
        sloppyOkvsType = config.getSloppyOkvsType();
        sigmaOkvsType = config.getSigmaOkvsType();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        oprfReceiver.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        oprfSender.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
        psuServer.setTaskId(taskIdPrf.getLong(2, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        oprfReceiver.setParallel(parallel);
        oprfSender.setParallel(parallel);
        psuServer.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        oprfReceiver.addLogLevel();
        oprfSender.addLogLevel();
        psuServer.addLogLevel();
    }

    @Override
    public void init(int maxServerSetSize, int maxClientSetSize, int maxK) throws MpcAbortException {
        setInitInput(maxServerSetSize, maxClientSetSize, maxK);
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int serverMaxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerSetSize);
        oprfReceiver.init(serverMaxBinNum);
        int clientMaxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientSetSize);
        oprfSender.init(clientMaxBinNum);
        psuServer.init(maxK * maxServerSetSize, maxK * maxClientSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        List<byte[]> serverKeysPayload = new LinkedList<>();
        // PID映射密钥
        pidMapPrfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(pidMapPrfKey);
        serverKeysPayload.add(pidMapPrfKey);
        // s^A（不用放在数据包中）
        serverPidPrfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(serverPidPrfKey);
        // PMID映射密钥
        pmidMapPrfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(pmidMapPrfKey);
        serverKeysPayload.add(pmidMapPrfKey);
        // σ映射密钥
        sigmaMapPrfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(sigmaMapPrfKey);
        serverKeysPayload.add(sigmaMapPrfKey);
        // 服务端PID的OKVS密钥
        int sloppyOkvsHashKeyNum = OkvsFactory.getHashNum(sloppyOkvsType);
        serverPidOkvsHashKeys = IntStream.range(0, sloppyOkvsHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] okvsKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(okvsKey);
                serverKeysPayload.add(okvsKey);
                return okvsKey;
            })
            .toArray(byte[][]::new);
        // σ的OKVS密钥
        int sigmaOkvsHashKeyNum = OkvsFactory.getHashNum(sigmaOkvsType);
        sigmaOkvsHashKeys = IntStream.range(0, sigmaOkvsHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] okvsKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(okvsKey);
                serverKeysPayload.add(okvsKey);
                return okvsKey;
            })
            .toArray(byte[][]::new);
        DataPacketHeader serverKeysHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverKeysHeader, serverKeysPayload));
        // 接收客户端密钥
        DataPacketHeader clientKeysHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientKeysPayload = rpc.receive(clientKeysHeader).getPayload();
        // 客户端PID的OKVS密钥
        MpcAbortPreconditions.checkArgument(clientKeysPayload.size() == sloppyOkvsHashKeyNum);
        clientPidOkvsHashKeys = clientKeysPayload.toArray(new byte[0][]);
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
        // PID字节长度等于λ + log(n) + log(m) = λ + log(m * n)
        pidByteLength = CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(
            LongUtils.ceilLog2((long) serverSetSize * clientSetSize)
        );
        pidMapPrf = PrfFactory.createInstance(envType, pidByteLength);
        pidMapPrf.setKey(pidMapPrfKey);
        serverPidPrf = PrfFactory.createInstance(envType, pidByteLength);
        serverPidPrf.setKey(serverPidPrfKey);
        // PMID字节长度等于λ + log(nk) + log(m) = λ + log(m * n * k)
        int pmidByteLength = CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(
            LongUtils.ceilLog2((long) k * serverSetSize * clientSetSize)
        );
        pmidMapPrf = PrfFactory.createInstance(envType, pmidByteLength);
        pmidMapPrf.setKey(pmidMapPrfKey);
        // σ = λ + Max{log(nk), log(mk)}
        sigma = CommonConstants.STATS_BYTE_LENGTH + Math.max(
            LongUtils.ceilLog2((long) k * serverSetSize), LongUtils.ceilLog2((long) k * clientSetSize)
        );
        sigmaMapPrf = PrfFactory.createInstance(envType, sigma);
        sigmaMapPrf.setKey(sigmaMapPrfKey);
        // Alice inserts items into cuckoo hash
        List<byte[]> serverCuckooHashKeyPayload = generateServerCuckooHashKeyPayload();
        DataPacketHeader serverCuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverCuckooHashKeyHeader, serverCuckooHashKeyPayload));
        stopWatch.stop();
        long serverCuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 1/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), serverCuckooHashTime);

        stopWatch.start();
        // The parties call F_{bOPRF}, where Alice is receiver with input A.
        // Alice receives output (f_1^A, ..., f_m^A), where f_j^A = PRF(k_j^B, x||i)
        byte[][] serverOprfInputs = IntStream.range(0, serverBinNum)
            .mapToObj(serverBinIndex -> {
                HashBinEntry<T> hashBinEntry = serverCuckooHashBin.getHashBinEntry(serverBinIndex);
                byte[] elementBytes = hashBinEntry.getItemByteArray();
                return ByteBuffer.allocate(elementBytes.length + Integer.BYTES)
                    .put(elementBytes)
                    .putInt(hashBinEntry.getHashIndex())
                    .array();
            })
            .toArray(byte[][]::new);
        oprfReceiverOutput = oprfReceiver.oprf(serverOprfInputs);
        stopWatch.stop();
        long serverOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 2/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), serverOprfTime);

        stopWatch.start();
        // Alice receives PID OKVS
        DataPacketHeader clientPidOkvsHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PID_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientPidOkvsPayload = rpc.receive(clientPidOkvsHeader).getPayload();
        serverPidMap = handleClientPidOkvsPayload(clientPidOkvsPayload);
        stopWatch.stop();
        long clientPidOkvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 3/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), clientPidOkvsTime);

        stopWatch.start();
        // Bob insert items into cuckoo hash
        DataPacketHeader clientCuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientCuckooHashKeyPayload = rpc.receive(clientCuckooHashKeyHeader).getPayload();
        handleClientCuckooHashKeyPayload(clientCuckooHashKeyPayload);
        stopWatch.stop();
        long clientCuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 4/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), clientCuckooHashTime);

        stopWatch.start();
        // The parties call F_{bOPRF}, where Alice is sender.
        // Alice receives output (k_1^A, ..., k_m^A)
        oprfSenderOutput = oprfSender.oprf(clientBinNum);
        stopWatch.stop();
        long clientOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 5/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), clientOprfTime);

        stopWatch.start();
        // Alice sends PID OKVS
        List<byte[]> serverPidOkvsPayload = generateServerPidOkvsPayload();
        DataPacketHeader serverPidOkvsHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PID_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPidOkvsHeader, serverPidOkvsPayload));
        stopWatch.stop();
        long serverPidOkvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 6/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), serverPidOkvsTime);

        stopWatch.start();
        // Alice defines k_{x_i}
        generateKxMap();
        stopWatch.stop();
        long kxMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 7/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), kxMapTime);

        stopWatch.start();
        // Alice receives SIGMA OKVS D from Bob.
        DataPacketHeader sigmaOkvsHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_SIGMA_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> sigmaOkvsPayload = rpc.receive(sigmaOkvsHeader).getPayload();
        // Alice computes idy_i^(j)
        Map<ByteBuffer, T> serverPmidMap = handleSigmaOkvsPayload(sigmaOkvsPayload);
        stopWatch.stop();
        long pmidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 8/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pmidMapTime);

        stopWatch.start();
        // 双方同步对方PSU的元素数量
        List<byte[]> serverPsuSetSizePayload = new LinkedList<>();
        serverPsuSetSizePayload.add(IntUtils.intToByteArray(serverPmidMap.size()));
        DataPacketHeader serverPsuSetSizeHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPsuSetSizeHeader, serverPsuSetSizePayload));

        DataPacketHeader clientPsuSetSizeHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
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
        info("{}{} Server Step 9/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), psuTime);

        stopWatch.start();
        // Alice receives union
        DataPacketHeader unionHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_UNION.ordinal(), extraInfo,
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
        info("{}{} Server Step 10/10 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), unionTime);

        return new PmidPartyOutput<>(pmidByteLength, pmidSet, serverPmidMap);
    }

    private List<byte[]> generateServerCuckooHashKeyPayload() {
        serverBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverSetSize);
        // 设置布谷鸟哈希，如果发现不能构造成功，则可以重复构造
        boolean success = false;
        byte[][] serverCuckooHashKeys = null;
        while (!success) {
            try {
                serverCuckooHashKeys = IntStream.range(0, cuckooHashNum)
                    .mapToObj(hashIndex -> {
                        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                        secureRandom.nextBytes(key);
                        return key;
                    })
                    .toArray(byte[][]::new);
                serverCuckooHashBin = CuckooHashBinFactory.createCuckooHashBin(
                    envType, cuckooHashBinType, serverSetSize, serverCuckooHashKeys
                );
                // 将服务端消息插入到CuckooHash中
                serverCuckooHashBin.insertItems(serverElementArrayList);
                if (serverCuckooHashBin.itemNumInStash() == 0) {
                    success = true;
                }
            } catch (ArithmeticException ignored) {
                // 如果插入不成功，就重新插入
            }
        }
        // 如果成功，则向布谷鸟哈希的空余位置插入随机元素
        serverCuckooHashBin.insertPaddingItems(secureRandom);
        return Arrays.stream(serverCuckooHashKeys).collect(Collectors.toList());
    }

    private Map<ByteBuffer, T> handleClientPidOkvsPayload(List<byte[]> clientPidOkvsPayload) throws MpcAbortException {
        int clientPidOkvsM = OkvsFactory.getM(sloppyOkvsType, clientSetSize * cuckooHashNum);
        MpcAbortPreconditions.checkArgument(clientPidOkvsPayload.size() == clientPidOkvsM);
        byte[][] clientOkvsStorage = clientPidOkvsPayload.toArray(new byte[0][]);
        Okvs<ByteBuffer> clientPidOkvs = OkvsFactory.createInstance(
            envType, sloppyOkvsType, clientSetSize * cuckooHashNum, pidByteLength * Byte.SIZE, clientPidOkvsHashKeys
        );
        IntStream serverBinIndexStream = IntStream.range(0, serverBinNum);
        serverBinIndexStream = parallel ? serverBinIndexStream.parallel() : serverBinIndexStream;
        ByteBuffer[] serverPids = serverBinIndexStream
            .mapToObj(serverBinIndex -> {
                HashBinEntry<T> hashBinEntry = serverCuckooHashBin.getHashBinEntry(serverBinIndex);
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
                // R^A(x) = P^B(x || i) ⊕ f^A_{h_i(x)} ⊕ PRF'(s^A, x)
                byte[] pidBytes = clientPidOkvs.decode(clientOkvsStorage, pidExtendElementBytes);
                BytesUtils.xori(pidBytes, pidMapPrf.getBytes(oprfReceiverOutput.getPrf(serverBinIndex)));
                BytesUtils.xori(pidBytes, serverPidPrf.getBytes(elementBytes));
                return ByteBuffer.wrap(pidBytes);
            })
            .toArray(ByteBuffer[]::new);
        Map<ByteBuffer, T> serverPidMap = new HashMap<>(serverSetSize);
        IntStream.range(0, serverBinNum).forEach(serverBinIndex -> {
            if (serverPids[serverBinIndex] != null) {
                serverPidMap.put(serverPids[serverBinIndex], serverCuckooHashBin.getHashBinEntry(serverBinIndex).getItem());
            }
        });
        serverCuckooHashBin = null;
        oprfReceiverOutput = null;
        return serverPidMap;
    }

    private void handleClientCuckooHashKeyPayload(List<byte[]> clientCuckooHashKeyPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientCuckooHashKeyPayload.size() == cuckooHashNum);
        clientBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientSetSize);
        clientCuckooHashes = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                byte[] key = clientCuckooHashKeyPayload.remove(0);
                Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
                hash.setKey(key);
                return hash;
            })
            .toArray(Prf[]::new);
    }

    private List<byte[]> generateServerPidOkvsPayload() {
        // 客户端字节元素
        ByteBuffer[] serverElementByteBuffers = serverElementArrayList.stream()
            .map(ObjectUtils::objectToByteArray)
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        // 客户端扩展字节元素
        ByteBuffer[][] serverExtendElementByteBuffers = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> Arrays.stream(serverElementByteBuffers)
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
        ByteBuffer[] serverPidOkvsKeyArray = Arrays.stream(serverExtendElementByteBuffers)
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
        byte[][] serverPidOkvsValueArray = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                // value值涉及密码学操作，并发处理
                IntStream serverElementIntStream = IntStream.range(0, serverSetSize);
                serverElementIntStream = parallel ? serverElementIntStream.parallel() : serverElementIntStream;
                return serverElementIntStream
                    .mapToObj(index -> {
                        byte[] elementBytes = serverElementByteBuffers[index].array();
                        byte[] extendElementBytes = serverExtendElementByteBuffers[hashIndex][index].array();
                        byte[] pid0 = serverPidPrf.getBytes(elementBytes);
                        int clientBinIndex = clientCuckooHashes[hashIndex].getInteger(elementBytes, clientBinNum);
                        byte[] pid1 = pidMapPrf.getBytes(oprfSenderOutput.getPrf(clientBinIndex, extendElementBytes));
                        BytesUtils.xori(pid0, pid1);
                        return pid0;
                    })
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
        Map<ByteBuffer, byte[]> serverPidOkvsKeyValueMap = new HashMap<>(serverSetSize * cuckooHashNum);
        IntStream.range(0, serverSetSize * cuckooHashNum).forEach(index ->
            serverPidOkvsKeyValueMap.put(serverPidOkvsKeyArray[index], serverPidOkvsValueArray[index])
        );
        Okvs<ByteBuffer> serverPidOkvs = OkvsFactory.createInstance(
            envType, sloppyOkvsType, serverSetSize * cuckooHashNum, pidByteLength * Byte.SIZE, serverPidOkvsHashKeys
        );
        // 编码可以并行处理
        serverPidOkvs.setParallelEncode(parallel);
        byte[][] serverPidOkvsStorage = serverPidOkvs.encode(serverPidOkvsKeyValueMap);
        return Arrays.stream(serverPidOkvsStorage).collect(Collectors.toList());
    }

    private void generateKxMap() {
        // Alice defines k_{x_i} = H(F_{k_B}(xi) || K + 1) for i ∈ [m]
        Stream<ByteBuffer> serverPidStream = serverPidMap.keySet().stream();
        serverPidStream = parallel ? serverPidStream.parallel() : serverPidStream;
        kxMap = serverPidStream
            .collect(Collectors.toMap(
                serverPid -> serverPid,
                serverPid -> {
                    byte[] pid = serverPid.array();
                    byte[] extendPid = ByteBuffer.allocate(pid.length + Integer.BYTES).put(pid).putInt(k + 1).array();
                    return sigmaMapPrf.getBytes(extendPid);
                })
            );
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
        Stream<ByteBuffer> serverPidStream = serverPidMap.keySet().stream();
        serverPidStream = parallel ? serverPidStream.parallel() : serverPidStream;
        serverPidStream.forEach(serverPid -> {
            T x = serverPidMap.get(serverPid);
            ByteBuffer sigmaX = ByteBuffer.wrap(sigmaMapPrf.getBytes(ObjectUtils.objectToByteArray(x)));
            byte[] serverPidBytes = serverPid.array();
            // Alice computes d_i = k_{x_i} ⊕ Decode_H(D, x_i) for i ∈ [m].
            byte[] kxBytes = kxMap.get(serverPid);
            byte[] dxBytes = sigmaOkvs.decode(sigmaOkvsStorage, sigmaX);
            BytesUtils.xori(dxBytes, kxBytes);
            BigInteger dxBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(dxBytes);
            if (BigIntegerUtils.lessOrEqual(dxBigInteger, kBigInteger)
                && BigIntegerUtils.greater(dxBigInteger, BigInteger.ONE)
            ) {
                int dx = dxBigInteger.intValue();
                // If 1 < d_i ≤ K, Alice computes id(x_i^(j)) = H(r^A(x_i) || j) for j ∈ [d_i];
                for (int j = 1; j <= dx; j++) {
                    byte[] extendServerPid = ByteBuffer.allocate(serverPidBytes.length + Integer.BYTES)
                        .put(serverPidBytes).putInt(j)
                        .array();
                    byte[] pmid = pmidMapPrf.getBytes(extendServerPid);
                    serverPmidMap.put(ByteBuffer.wrap(pmid), x);
                }
            } else {
                // else, Alice computes id(x_i^(1)) = H(r^A(x_i) || 1)
                byte[] extendServerPid = ByteBuffer.allocate(serverPidBytes.length + Integer.BYTES)
                    .put(serverPidBytes).putInt(1)
                    .array();
                byte[] pmid = pmidMapPrf.getBytes(extendServerPid);
                serverPmidMap.put(ByteBuffer.wrap(pmid), x);
            }
        });
        return serverPmidMap;
    }
}
