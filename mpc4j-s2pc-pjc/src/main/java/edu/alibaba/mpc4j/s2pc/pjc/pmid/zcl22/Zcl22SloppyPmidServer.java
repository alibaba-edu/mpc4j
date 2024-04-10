package edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.*;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.AbstractPmidServer;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22.Zcl22SloppyPmidPtoDesc.PtoStep;
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
    private final Gf2eDokvsType sloppyOkvsType;
    /**
     * σ的OKVS类型
     */
    private final Gf2eDokvsType sigmaOkvsType;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * 布谷鸟哈希函数数量
     */
    private final int cuckooHashNum;
    /**
     * 服务端PID的PRF密钥
     */
    private byte[] serverPidPrfKey;
    /**
     * 服务端PID的OKVS密钥
     */
    private byte[][] serverSloppyOkvsHashKeys;
    /**
     * 客户端PID的OKVS密钥
     */
    private byte[][] clientSloppyOkvsHashKeys;
    /**
     * 服务端σ的OKVS密钥
     */
    private byte[][] serverSigmaOkvsHashKeys;
    /**
     * 客户端σ的OKVS密钥
     */
    private byte[][] clientSigmaOkvsHashKeys;
    /**
     * PID字节长度
     */
    private int pidByteLength;
    /**
     * PMID字节长度
     */
    private int pmidByteLength;
    /**
     * PID映射函数
     */
    private Hash pidMap;
    /**
     * 服务端PID伪随机函数
     */
    private Prf serverPidPrf;
    /**
     * PMID映射伪随机函数
     */
    private Hash pmidMap;
    /**
     * σ的OKVS值字节长度
     */
    private int sigmaOkvsValueByteLength;
    /**
     * σ的OKVS值映射函数
     */
    private Hash sigmaOkvsValueMap;
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
    private OprfReceiverOutput kbOprfOutput;
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
    private OprfSenderOutput kaOprfKey;
    /**
     * 服务端PID映射
     */
    private Map<ByteBuffer, T> serverPidMap;
    /**
     * q^A
     */
    private Map<ByteBuffer, byte[]> qxMap;
    /**
     * dxMap
     */
    private Map<ByteBuffer, Integer> dxMap;

    public Zcl22SloppyPmidServer(Rpc serverRpc, Party clientParty, Zcl22SloppyPmidConfig config) {
        super(Zcl22SloppyPmidPtoDesc.getInstance(), serverRpc, clientParty, config);
        oprfReceiver = OprfFactory.createOprfReceiver(serverRpc, clientParty, config.getOprfConfig());
        addSubPto(oprfReceiver);
        oprfSender = OprfFactory.createOprfSender(serverRpc, clientParty, config.getOprfConfig());
        addSubPto(oprfSender);
        psuServer = PsuFactory.createServer(serverRpc, clientParty, config.getPsuConfig());
        addSubPto(psuServer);
        sloppyOkvsType = config.getSloppyOkvsType();
        sigmaOkvsType = config.getSigmaOkvsType();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxServerSetSize, int maxServerU, int maxClientSetSize, int maxClientU) throws MpcAbortException {
        setInitInput(maxServerSetSize, maxServerU, maxClientSetSize, maxClientU);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int serverMaxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerSetSize);
        oprfReceiver.init(serverMaxBinNum);
        int clientMaxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientSetSize);
        oprfSender.init(clientMaxBinNum);
        psuServer.init(maxServerU * maxClientU * maxServerSetSize, maxServerU * maxClientU * maxClientSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, initTime);

        stopWatch.start();
        // s^A
        serverPidPrfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(serverPidPrfKey);
        List<byte[]> serverKeysPayload = new LinkedList<>();
        // 服务端PID的OKVS密钥
        int sloppyOkvsHashKeyNum = Gf2eDokvsFactory.getHashKeyNum(sloppyOkvsType);
        serverSloppyOkvsHashKeys = IntStream.range(0, sloppyOkvsHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] okvsKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(okvsKey);
                serverKeysPayload.add(okvsKey);
                return okvsKey;
            })
            .toArray(byte[][]::new);
        // 服务端σ的OKVS密钥
        int sigmaOkvsHashKeyNum = Gf2eDokvsFactory.getHashKeyNum(sigmaOkvsType);
        serverSigmaOkvsHashKeys = IntStream.range(0, sigmaOkvsHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] serverSigmaOkvsHashKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(serverSigmaOkvsHashKey);
                serverKeysPayload.add(serverSigmaOkvsHashKey);
                return serverSigmaOkvsHashKey;
            })
            .toArray(byte[][]::new);
        DataPacketHeader serverKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverKeysHeader, serverKeysPayload));
        stopWatch.stop();
        long serverKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, serverKeyTime);

        stopWatch.start();
        // 接收客户端密钥
        DataPacketHeader clientKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientKeysPayload = rpc.receive(clientKeysHeader).getPayload();
        // 客户端PID的OKVS密钥、客户端σ的OKVS密钥
        MpcAbortPreconditions.checkArgument(clientKeysPayload.size() == sloppyOkvsHashKeyNum + sigmaOkvsHashKeyNum);
        clientSloppyOkvsHashKeys = IntStream.range(0, sloppyOkvsHashKeyNum)
            .mapToObj(hashIndex -> clientKeysPayload.remove(0))
            .toArray(byte[][]::new);
        // 客户端σ的OKVS密钥
        clientSigmaOkvsHashKeys = clientKeysPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long clientKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, clientKeyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PmidPartyOutput<T> pmid(Set<T> serverElementSet, int clientSetSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientSetSize);
        return pmid();
    }

    @Override
    public PmidPartyOutput<T> pmid(Set<T> serverElementSet, int clientSetSize, int clientU) throws MpcAbortException {
        setPtoInput(serverElementSet, clientSetSize, clientU);
        return pmid();
    }

    @Override
    public PmidPartyOutput<T> pmid(Map<T, Integer> serverElementMap, int clientSetSize) throws MpcAbortException {
        setPtoInput(serverElementMap, clientSetSize);
        return pmid();
    }

    @Override
    public PmidPartyOutput<T> pmid(Map<T, Integer> serverElementMap, int clientSetSize, int clientU) throws MpcAbortException {
        setPtoInput(serverElementMap, clientSetSize, clientU);
        return pmid();
    }

    private PmidPartyOutput<T> pmid() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        initVariables();
        stopWatch.stop();
        long initVariableTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, initVariableTime);

        stopWatch.start();
        generateServerPidMap();
        stopWatch.stop();
        long serverPidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, serverPidMapTime);

        stopWatch.start();
        if (serverU == 1 && clientU == 1) {
            clientEmptySigmaOkvs();
        } else if (serverU == 1) {
            generateQxMap();
            clientSigmaOkvs();
        } else if (clientU == 1) {
            generateQxMap();
            serverSigmaOkvs();
            clientEmptySigmaOkvs();
        } else {
            generateQxMap();
            serverSigmaOkvs();
            clientSigmaOkvs();
        }
        stopWatch.stop();
        long sigmaOkvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, sigmaOkvsTime);

        stopWatch.start();
        // Alice computes id(y_i)
        Map<ByteBuffer, T> serverPmidMap = generateServerPmidMap();
        stopWatch.stop();
        long pmidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, pmidMapTime);

        stopWatch.start();
        Set<ByteBuffer> pmidSet = union(serverPmidMap);
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, unionTime);

        logPhaseInfo(PtoState.PTO_END);
        return new PmidPartyOutput<>(pmidByteLength, pmidSet, serverPmidMap);
    }

    private void initVariables() throws MpcAbortException {
        pidByteLength = PidUtils.getPidByteLength(serverSetSize, clientSetSize);
        pidMap = HashFactory.createInstance(envType, pidByteLength);
        serverPidPrf = PrfFactory.createInstance(envType, pidByteLength);
        serverPidPrf.setKey(serverPidPrfKey);
        pmidByteLength = PmidUtils.getPmidByteLength(serverSetSize, serverU, clientSetSize, clientU);
        pmidMap = HashFactory.createInstance(envType, pmidByteLength);
        sigmaOkvsValueByteLength = Zcl22PmidUtils.getSigmaOkvsValueByteLength(
            serverSetSize, serverU, clientSetSize, clientU
        );
        sigmaOkvsValueMap = HashFactory.createInstance(envType, sigmaOkvsValueByteLength);
        // Alice inserts items into cuckoo hash
        List<byte[]> serverCuckooHashKeyPayload = generateServerCuckooHashKeyPayload();
        DataPacketHeader serverCuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverCuckooHashKeyHeader, serverCuckooHashKeyPayload));
        // Bob insert items into cuckoo hash
        DataPacketHeader clientCuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientCuckooHashKeyPayload = rpc.receive(clientCuckooHashKeyHeader).getPayload();
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

    private List<byte[]> generateServerCuckooHashKeyPayload() {
        serverBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverSetSize);
        serverCuckooHashBin = CuckooHashBinFactory.createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, serverSetSize, serverElementArrayList, secureRandom
        );
        serverCuckooHashBin.insertPaddingItems(secureRandom);
        return Arrays.stream(serverCuckooHashBin.getHashKeys()).collect(Collectors.toList());
    }

    private void generateServerPidMap() throws MpcAbortException {
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
        kbOprfOutput = oprfReceiver.oprf(serverOprfInputs);
        // The parties call F_{bOPRF}, where Alice is sender.
        // Alice receives output (k_1^A, ..., k_m^A)
        kaOprfKey = oprfSender.oprf(clientBinNum);
        // Alice sends PID OKVS
        List<byte[]> serverPidOkvsPayload = generateServerPidOkvsPayload();
        DataPacketHeader serverPidOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PID_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPidOkvsHeader, serverPidOkvsPayload));
        // Alice receives PID OKVS
        DataPacketHeader clientPidOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PID_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientPidOkvsPayload = rpc.receive(clientPidOkvsHeader).getPayload();
        serverPidMap = handleClientPidOkvsPayload(clientPidOkvsPayload);
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
                    .map(pidMap::digestToBytes)
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
                        byte[] pid1 = pidMap.digestToBytes(kaOprfKey.getPrf(clientBinIndex, extendElementBytes));
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
        Gf2eDokvs<ByteBuffer> serverPidOkvs = Gf2eDokvsFactory.createInstance(
            envType, sloppyOkvsType, serverSetSize * cuckooHashNum, pidByteLength * Byte.SIZE, serverSloppyOkvsHashKeys
        );
        // 编码可以并行处理
        serverPidOkvs.setParallelEncode(parallel);
        byte[][] serverPidOkvsStorage = serverPidOkvs.encode(serverPidOkvsKeyValueMap, false);
        return Arrays.stream(serverPidOkvsStorage).collect(Collectors.toList());
    }

    private Map<ByteBuffer, T> handleClientPidOkvsPayload(List<byte[]> clientPidOkvsPayload) throws MpcAbortException {
        int clientPidOkvsM = Gf2eDokvsFactory.getM(envType, sloppyOkvsType, clientSetSize * cuckooHashNum);
        MpcAbortPreconditions.checkArgument(clientPidOkvsPayload.size() == clientPidOkvsM);
        byte[][] clientOkvsStorage = clientPidOkvsPayload.toArray(new byte[0][]);
        Gf2eDokvs<ByteBuffer> clientPidOkvs = Gf2eDokvsFactory.createInstance(
            envType, sloppyOkvsType, clientSetSize * cuckooHashNum, pidByteLength * Byte.SIZE, clientSloppyOkvsHashKeys
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
                ByteBuffer pidExtendElementBytes = ByteBuffer.wrap(pidMap.digestToBytes(extendElementBytes));
                // R^A(x) = P^B(x || i) ⊕ f^A_{h_i(x)} ⊕ PRF'(s^A, x)
                byte[] pidBytes = clientPidOkvs.decode(clientOkvsStorage, pidExtendElementBytes);
                BytesUtils.xori(pidBytes, pidMap.digestToBytes(kbOprfOutput.getPrf(serverBinIndex)));
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
        kbOprfOutput = null;
        return serverPidMap;
    }

    private void generateQxMap() {
        // Alice defines q_{x_i}^A = H(r^A(x_i) || 0) for i ∈ [m]
        Stream<ByteBuffer> serverPidStream = serverPidMap.keySet().stream();
        serverPidStream = parallel ? serverPidStream.parallel() : serverPidStream;
        qxMap = serverPidStream
            .collect(Collectors.toMap(
                serverPid -> serverPid,
                serverPid -> {
                    byte[] pid = serverPid.array();
                    byte[] extendPid = ByteBuffer.allocate(pid.length + Integer.BYTES).put(pid).putInt(0).array();
                    return sigmaOkvsValueMap.digestToBytes(extendPid);
                })
            );
    }

    private void clientEmptySigmaOkvs() {
        // 客户端没有重数，dx = 1
        Stream<ByteBuffer> serverPidStream = serverPidMap.keySet().stream();
        serverPidStream = parallel ? serverPidStream.parallel() : serverPidStream;
        dxMap = serverPidStream.collect(Collectors.toMap(
            serverPid -> serverPid,
            serverPid -> 1
            )
        );
    }

    private void clientSigmaOkvs() throws MpcAbortException {
        // Alice receives σ-OKVS^B
        DataPacketHeader clientSigmaOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_SIGMA_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientSigmaOkvsPayload = rpc.receive(clientSigmaOkvsHeader).getPayload();
        int clientSigmaOkvsM = Gf2eDokvsFactory.getM(envType, sigmaOkvsType, clientSetSize);
        MpcAbortPreconditions.checkArgument(clientSigmaOkvsPayload.size() == clientSigmaOkvsM);
        // 读取OKVS^B
        byte[][] clientSigmaOkvsStorage = clientSigmaOkvsPayload.toArray(new byte[0][]);
        Gf2eDokvs<ByteBuffer> clientSigmaOkvs = Gf2eDokvsFactory.createInstance(
            envType, sigmaOkvsType, clientSetSize, sigmaOkvsValueByteLength * Byte.SIZE, clientSigmaOkvsHashKeys
        );
        // 初始化必要的参数
        BigInteger clientBigIntegerU = BigInteger.valueOf(clientU);
        Stream<ByteBuffer> serverPidStream = serverPidMap.keySet().stream();
        serverPidStream = parallel ? serverPidStream.parallel() : serverPidStream;
        dxMap = serverPidStream.collect(Collectors.toMap(
            serverPid -> serverPid,
            serverPid -> {
                T x = serverPidMap.get(serverPid);
                ByteBuffer xi = ByteBuffer.wrap(sigmaOkvsValueMap.digestToBytes(ObjectUtils.objectToByteArray(x)));
                // Alice computes d_i = q_{x_i} ⊕ Decode_H(D, x_i) for i ∈ [m].
                byte[] qxBytes = qxMap.get(serverPid);
                byte[] dxBytes = clientSigmaOkvs.decode(clientSigmaOkvsStorage, xi);
                BytesUtils.xori(dxBytes, qxBytes);
                BigInteger dxBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(dxBytes);
                if (BigIntegerUtils.lessOrEqual(dxBigInteger, clientBigIntegerU) && BigIntegerUtils.greater(dxBigInteger, BigInteger.ONE)) {
                    // If 1 < d_i ≤ clientU, Alice set d_i = d_i
                    return dxBigInteger.intValue();
                } else {
                    // else, Alice set d_i = 1
                    return 1;
                }
            })
        );
    }

    private void serverSigmaOkvs() {
        // Alice computes an σ-OKVS^A
        Stream<ByteBuffer> serverPidStream = serverPidMap.keySet().stream();
        serverPidStream = parallel ? serverPidStream.parallel() : serverPidStream;
        Map<ByteBuffer, byte[]> serverSigmaKeyValueMap = serverPidStream
            .collect(Collectors.toMap(
                // key = x_i
                serverPid -> {
                    T x = serverPidMap.get(serverPid);
                    return ByteBuffer.wrap(sigmaOkvsValueMap.digestToBytes(ObjectUtils.objectToByteArray(x)));
                },
                // value = q_{x_i} ⊕ c_{x_i}
                serverPid -> {
                    T x = serverPidMap.get(serverPid);
                    int ux = serverElementMap.get(x);
                    byte[] dx;
                    if (ux == 1) {
                        // if u_j = 1, Bob selects a random c_j ← {0, 1}^σ
                        dx = new byte[sigmaOkvsValueByteLength];
                        secureRandom.nextBytes(dx);
                    } else {
                        // else defines c_j = u_j
                        dx = IntUtils.nonNegIntToFixedByteArray(ux, sigmaOkvsValueByteLength);
                    }
                    BytesUtils.xori(dx, qxMap.get(serverPid));
                    return dx;
                }
            ));
        Gf2eDokvs<ByteBuffer> serverSigmaOkvs = Gf2eDokvsFactory.createInstance(
            envType, sigmaOkvsType, serverSetSize, sigmaOkvsValueByteLength * Byte.SIZE, serverSigmaOkvsHashKeys
        );
        // OKVS编码可以并行处理
        serverSigmaOkvs.setParallelEncode(parallel);
        byte[][] serverSigmaOkvsStorage = serverSigmaOkvs.encode(serverSigmaKeyValueMap, false);
        List<byte[]> serverSigmaOkvsPayload = Arrays.stream(serverSigmaOkvsStorage).collect(Collectors.toList());
        DataPacketHeader serverSigmaOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SIGMA_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverSigmaOkvsHeader, serverSigmaOkvsPayload));
    }

    private Map<ByteBuffer, T> generateServerPmidMap() {
        Map<ByteBuffer, T> serverPmidMap = new ConcurrentHashMap<>(serverSetSize * serverU * clientU);
        Stream<ByteBuffer> serverPidStream = serverPidMap.keySet().stream();
        serverPidStream = parallel ? serverPidStream.parallel() : serverPidStream;
        serverPidStream.forEach(serverPid -> {
            T x = serverPidMap.get(serverPid);
            for (int j = 1; j <= serverElementMap.get(x) * dxMap.get(serverPid); j++) {
                byte[] serverPidBytes = serverPid.array();
                byte[] extendServerPid = ByteBuffer.allocate(serverPidBytes.length + Integer.BYTES)
                    .put(serverPidBytes)
                    .putInt(j)
                    .array();
                byte[] pmid = pmidMap.digestToBytes(extendServerPid);
                serverPmidMap.put(ByteBuffer.wrap(pmid), x);
            }
        });
        qxMap = null;
        serverPidMap = null;
        dxMap = null;
        return serverPmidMap;
    }

    private Set<ByteBuffer> union(Map<ByteBuffer, T> serverPmidMap) throws MpcAbortException {
        // 双方同步对方PSU的元素数量
        List<byte[]> serverPsuSetSizePayload = new LinkedList<>();
        serverPsuSetSizePayload.add(IntUtils.intToByteArray(serverPmidMap.size()));
        DataPacketHeader serverPsuSetSizeHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPsuSetSizeHeader, serverPsuSetSizePayload));

        DataPacketHeader clientPsuSetSizeHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientPsuSetSizePayload = rpc.receive(clientPsuSetSizeHeader).getPayload();
        MpcAbortPreconditions.checkArgument(clientPsuSetSizePayload.size() == 1);
        int clientPsuSetSize = IntUtils.byteArrayToInt(clientPsuSetSizePayload.remove(0));
        // Alice and Bob invoke the PSU functionality F_{psu}. Alice acts as sender with input ID_x.
        psuServer.psu(serverPmidMap.keySet(), clientPsuSetSize, pmidByteLength);
        // Alice receives union
        DataPacketHeader unionHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_UNION.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> unionPayload = rpc.receive(unionHeader).getPayload();
        MpcAbortPreconditions.checkArgument(unionPayload.size() >= serverSetSize);
        return unionPayload.stream()
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());

    }
}
