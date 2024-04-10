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
import edu.alibaba.mpc4j.s2pc.pjc.pmid.AbstractPmidClient;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22.Zcl22SloppyPmidPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
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
 * ZCL22宽松PMID协议客户端。
 *
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
     * 客户端PID映射密钥
     */
    private byte[] clientPidPrfKey;
    /**
     * 客户端PID的OKVS密钥
     */
    private byte[][] clientSloppyOkvsHashKeys;
    /**
     * 服务端PID的OKVS密钥
     */
    private byte[][] serverSloppyOkvsHashKeys;
    /**
     * 客户端σ的OKVS密钥
     */
    private byte[][] clientSigmaOkvsHashKeys;
    /**
     * 服务端σ的OKVS密钥
     */
    private byte[][] serverSigmaOkvsHashKeys;
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
     * 客户端PID伪随机函数
     */
    private Prf clientPidPrf;
    /**
     * PMID映射函数
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
     * 服务端布谷鸟哈希
     */
    private Prf[] serverCuckooHashes;
    /**
     * (k_1^B, ..., k_m^B)
     */
    private OprfSenderOutput kbOprfKey;
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
    private OprfReceiverOutput kaOprfOutput;
    /**
     * 服务端PID映射
     */
    private Map<ByteBuffer, T> clientPidMap;
    /**
     * q^B
     */
    private Map<ByteBuffer, byte[]> qyMap;
    /**
     * dyMap
     */
    private Map<ByteBuffer, Integer> dyMap;

    public Zcl22SloppyPmidClient(Rpc clientRpc, Party serverParty, Zcl22SloppyPmidConfig config) {
        super(Zcl22SloppyPmidPtoDesc.getInstance(), clientRpc, serverParty, config);
        oprfSender = OprfFactory.createOprfSender(clientRpc, serverParty, config.getOprfConfig());
        addSubPto(oprfSender);
        oprfReceiver = OprfFactory.createOprfReceiver(clientRpc, serverParty, config.getOprfConfig());
        addSubPto(oprfReceiver);
        psuClient = PsuFactory.createClient(clientRpc, serverParty, config.getPsuConfig());
        addSubPto(psuClient);
        sloppyOkvsType = config.getSloppyOkvsType();
        sigmaOkvsType = config.getSigmaOkvsType();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxClientSetSize, int maxClientU, int maxServerSetSize, int maxServerU) throws MpcAbortException {
        setInitInput(maxClientSetSize, maxClientU, maxServerSetSize, maxServerU);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxServerBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerSetSize);
        oprfSender.init(maxServerBinNum);
        int maxClientBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientSetSize);
        oprfReceiver.init(maxClientBinNum);
        psuClient.init(maxServerU * maxClientU * maxClientSetSize, maxServerU * maxClientU * maxServerSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, initTime);

        stopWatch.start();
        // s^B
        clientPidPrfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(clientPidPrfKey);
        List<byte[]> clientKeysPayload = new LinkedList<>();
        // 客户端PID的OKVS密钥
        int sloppyOkvsHashKeyNum = Gf2eDokvsFactory.getHashKeyNum(sloppyOkvsType);
        clientSloppyOkvsHashKeys = IntStream.range(0, sloppyOkvsHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] okvsKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(okvsKey);
                clientKeysPayload.add(okvsKey);
                return okvsKey;
            })
            .toArray(byte[][]::new);
        // 客户端σ的OKVS密钥
        int sigmaOkvsHashKeyNum = Gf2eDokvsFactory.getHashKeyNum(sigmaOkvsType);
        clientSigmaOkvsHashKeys = IntStream.range(0, sigmaOkvsHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] clientSigmaOkvsHashKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(clientSigmaOkvsHashKey);
                clientKeysPayload.add(clientSigmaOkvsHashKey);
                return clientSigmaOkvsHashKey;
            })
            .toArray(byte[][]::new);
        DataPacketHeader clientKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientKeysHeader, clientKeysPayload));
        stopWatch.stop();
        long clientKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, clientKeyTime);

        stopWatch.start();
        // 接收服务端密钥
        DataPacketHeader serverKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverKeysPayload = rpc.receive(serverKeysHeader).getPayload();
        // 服务端PID的OKVS密钥、服务端σ的OKVS密钥
        MpcAbortPreconditions.checkArgument(serverKeysPayload.size() == sloppyOkvsHashKeyNum + sigmaOkvsHashKeyNum);
        serverSloppyOkvsHashKeys = IntStream.range(0, sloppyOkvsHashKeyNum)
            .mapToObj(hashIndex -> serverKeysPayload.remove(0))
            .toArray(byte[][]::new);
        serverSigmaOkvsHashKeys = serverKeysPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long serverKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, serverKeyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PmidPartyOutput<T> pmid(Set<T> clientElementSet, int serverSetSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverSetSize);
        return pmid();
    }

    @Override
    public PmidPartyOutput<T> pmid(Map<T, Integer> clientElementMap, int serverSetSize) throws MpcAbortException {
        setPtoInput(clientElementMap, serverSetSize);
        return pmid();
    }

    @Override
    public PmidPartyOutput<T> pmid(Set<T> clientElementSet, int serverSetSize, int serverU) throws MpcAbortException {
        setPtoInput(clientElementSet, serverSetSize, serverU);
        return pmid();
    }

    @Override
    public PmidPartyOutput<T> pmid(Map<T, Integer> clientElementMap, int serverSetSize, int serverU) throws MpcAbortException {
        setPtoInput(clientElementMap, serverSetSize, serverU);
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
        generateClientPidMap();
        stopWatch.stop();
        long clientPidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, clientPidMapTime);

        stopWatch.start();
        if (serverU == 1 && clientU == 1) {
            serverEmptySigmaOkvs();
        } else if (serverU == 1) {
            generateQyMap();
            clientSigmaOkvs();
            serverEmptySigmaOkvs();
        } else if (clientU == 1) {
            generateQyMap();
            serverSigmaOkvs();
        } else {
            generateQyMap();
            clientSigmaOkvs();
            serverSigmaOkvs();
        }
        long sigmaOkvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, sigmaOkvsTime);

        stopWatch.start();
        // Bob computes id(y_i)
        Map<ByteBuffer, T> clientPmidMap = generateClientPmidMap();
        stopWatch.stop();
        long pmidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, pmidMapTime);

        stopWatch.start();
        Set<ByteBuffer> pmidSet = union(clientPmidMap);
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, unionTime);

        logPhaseInfo(PtoState.PTO_END);
        return new PmidPartyOutput<>(pmidByteLength, pmidSet, clientPmidMap);
    }

    private void initVariables() throws MpcAbortException {
        // PID字节长度等于λ + log(n) + log(m) = λ + log(m * n)
        pidByteLength = PidUtils.getPidByteLength(serverSetSize, clientSetSize);
        pidMap = HashFactory.createInstance(envType, pidByteLength);
        clientPidPrf = PrfFactory.createInstance(envType, pidByteLength);
        clientPidPrf.setKey(clientPidPrfKey);
        pmidByteLength = PmidUtils.getPmidByteLength(serverSetSize, serverU, clientSetSize, clientU);
        pmidMap = HashFactory.createInstance(envType, pmidByteLength);
        // σ的OKVS值长度 = λ + Max{log(m * clientU), log(n * serverU)}
        sigmaOkvsValueByteLength = Zcl22PmidUtils.getSigmaOkvsValueByteLength(
            serverSetSize, serverU, clientSetSize, clientU
        );
        sigmaOkvsValueMap = HashFactory.createInstance(envType, sigmaOkvsValueByteLength);
        // Bob insert items into cuckoo hash
        List<byte[]> clientCuckooHashKeyPayload = generateClientCuckooHashKeyPayload();
        DataPacketHeader clientCuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientCuckooHashKeyHeader, clientCuckooHashKeyPayload));
        // Alice inserts items into cuckoo hash
        DataPacketHeader serverCuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverCuckooHashKeyPayload = rpc.receive(serverCuckooHashKeyHeader).getPayload();
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

    private List<byte[]> generateClientCuckooHashKeyPayload() {
        clientBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientSetSize);
        clientCuckooHashBin = CuckooHashBinFactory.createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, clientSetSize, clientElementArrayList, secureRandom
        );
        clientCuckooHashBin.insertPaddingItems(secureRandom);
        return Arrays.stream(clientCuckooHashBin.getHashKeys()).collect(Collectors.toList());
    }

    private void generateClientPidMap() throws MpcAbortException {
        // The parties call F_{bOPRF}, where Bob is sender.
        // Bob receives output (k_1^B, ..., k_m^B)
        kbOprfKey = oprfSender.oprf(serverBinNum);
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
        kaOprfOutput = oprfReceiver.oprf(clientOprfInputs);
        // Bob sends PID OKVS
        List<byte[]> clientPidOkvsPayload = generateClientPidOkvsPayload();
        DataPacketHeader clientPidOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PID_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPidOkvsHeader, clientPidOkvsPayload));
        // Bob receives PID OKVS
        DataPacketHeader serverPidOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PID_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverOkvsPayload = rpc.receive(serverPidOkvsHeader).getPayload();
        clientPidMap = handleServerPidOkvsPayload(serverOkvsPayload);
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
                    .map(pidMap::digestToBytes)
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
                        byte[] pid1 = pidMap.digestToBytes(kbOprfKey.getPrf(serverBinIndex, extendElementBytes));
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
        Gf2eDokvs<ByteBuffer> clientPidOkvs = Gf2eDokvsFactory.createInstance(
            envType, sloppyOkvsType, clientSetSize * cuckooHashNum, pidByteLength * Byte.SIZE, clientSloppyOkvsHashKeys
        );
        // 编码可以并行处理
        clientPidOkvs.setParallelEncode(parallel);
        byte[][] clientPidOkvsStorage = clientPidOkvs.encode(clientPidOkvsKeyValueMap, false);
        kbOprfKey = null;
        return Arrays.stream(clientPidOkvsStorage).collect(Collectors.toList());
    }

    private Map<ByteBuffer, T> handleServerPidOkvsPayload(List<byte[]> serverPidOkvsPayload) throws MpcAbortException {
        int serverOkvsM = Gf2eDokvsFactory.getM(envType, sloppyOkvsType, serverSetSize * cuckooHashNum);
        MpcAbortPreconditions.checkArgument(serverPidOkvsPayload.size() == serverOkvsM);
        byte[][] serverOkvsStorage = serverPidOkvsPayload.toArray(new byte[0][]);
        Gf2eDokvs<ByteBuffer> serverOkvs = Gf2eDokvsFactory.createInstance(
            envType, sloppyOkvsType, serverSetSize * cuckooHashNum, pidByteLength * Byte.SIZE, serverSloppyOkvsHashKeys
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
                ByteBuffer pidExtendElementBytes = ByteBuffer.wrap(pidMap.digestToBytes(extendElementBytes));
                // R^B(y) = P^A(y || i) ⊕ f^B_{h_i(y)} ⊕ PRF'(s^B, y)
                byte[] pidBytes = serverOkvs.decode(serverOkvsStorage, pidExtendElementBytes);
                BytesUtils.xori(pidBytes, pidMap.digestToBytes(kaOprfOutput.getPrf(clientBinIndex)));
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
        kaOprfOutput = null;
        return clientPidMap;
    }

    private void generateQyMap() {
        // Bob defines q_{y_i}^B = H(r^B(y_i) || 0) for i ∈ [n]
        Stream<ByteBuffer> clientPidStream = clientPidMap.keySet().stream();
        clientPidStream = parallel ? clientPidStream.parallel() : clientPidStream;
        qyMap = clientPidStream
            .collect(Collectors.toMap(
                clientPid -> clientPid,
                clientPid -> {
                    byte[] pid = clientPid.array();
                    byte[] extendPid = ByteBuffer.allocate(pid.length + Integer.BYTES).put(pid).putInt(0).array();
                    return sigmaOkvsValueMap.digestToBytes(extendPid);
                })
            );
    }

    private void clientSigmaOkvs() {
        // Bob computes an σ-OKVS^B
        Stream<ByteBuffer> clientPidStream = clientPidMap.keySet().stream();
        clientPidStream = parallel ? clientPidStream.parallel() : clientPidStream;
        Map<ByteBuffer, byte[]> clientSigmaKeyValueMap = clientPidStream
            .collect(Collectors.toMap(
                // key = y_i
                clientPid -> {
                    T y = clientPidMap.get(clientPid);
                    return ByteBuffer.wrap(sigmaOkvsValueMap.digestToBytes(ObjectUtils.objectToByteArray(y)));
                },
                // value = q_{y_i} ⊕ c_{y_i}
                clientPid -> {
                    T y = clientPidMap.get(clientPid);
                    int uy = clientElementMap.get(y);
                    byte[] dy;
                    if (uy == 1) {
                        // if u_j = 1, Bob selects a random c_j ← {0, 1}^σ
                        dy = new byte[sigmaOkvsValueByteLength];
                        secureRandom.nextBytes(dy);
                    } else {
                        // else defines c_j = u_j
                        dy = IntUtils.nonNegIntToFixedByteArray(uy, sigmaOkvsValueByteLength);
                    }
                    BytesUtils.xori(dy, qyMap.get(clientPid));
                    return dy;
                }
            ));
        Gf2eDokvs<ByteBuffer> clientSigmaOkvs = Gf2eDokvsFactory.createInstance(
            envType, sigmaOkvsType, clientSetSize, sigmaOkvsValueByteLength * Byte.SIZE, clientSigmaOkvsHashKeys
        );
        // OKVS编码可以并行处理
        clientSigmaOkvs.setParallelEncode(parallel);
        byte[][] clientSigmaOkvsStorage = clientSigmaOkvs.encode(clientSigmaKeyValueMap, false);
        List<byte[]> clientSigmaOkvsPayload = Arrays.stream(clientSigmaOkvsStorage).collect(Collectors.toList());
        DataPacketHeader clientSigmaOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_SIGMA_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientSigmaOkvsHeader, clientSigmaOkvsPayload));
    }

    private void serverEmptySigmaOkvs() {
        // 服务端没有重数，设置dy = 1
        dyMap = clientPidMap.keySet().stream().collect(Collectors.toMap(
            clientPid -> clientPid,
            clientPid -> 1
        ));
    }

    private void serverSigmaOkvs() throws MpcAbortException {
        // Bob receives σ-OKVS^A
        DataPacketHeader serverSigmaOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SIGMA_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverSigmaOkvsPayload = rpc.receive(serverSigmaOkvsHeader).getPayload();
        int serverSigmaOkvsM = Gf2eDokvsFactory.getM(envType, sigmaOkvsType, serverSetSize);
        MpcAbortPreconditions.checkArgument(serverSigmaOkvsPayload.size() == serverSigmaOkvsM);
        // 读取OKVS^A
        byte[][] serverSigmaOkvsStorage = serverSigmaOkvsPayload.toArray(new byte[0][]);
        Gf2eDokvs<ByteBuffer> serverSigmaOkvs = Gf2eDokvsFactory.createInstance(
            envType, sigmaOkvsType, serverSetSize, sigmaOkvsValueByteLength * Byte.SIZE, serverSigmaOkvsHashKeys
        );
        // 初始化必要的参数
        BigInteger serverBigIntegerU = BigInteger.valueOf(serverU);
        Stream<ByteBuffer> clientPidStream = clientPidMap.keySet().stream();
        clientPidStream = parallel ? clientPidStream.parallel() : clientPidStream;
        dyMap = clientPidStream.collect(Collectors.toMap(
            clientPid -> clientPid,
            clientPid -> {
                T y = clientPidMap.get(clientPid);
                ByteBuffer yi = ByteBuffer.wrap(sigmaOkvsValueMap.digestToBytes(ObjectUtils.objectToByteArray(y)));
                // Alice computes d_i = q_{y_i} ⊕ Decode_H(D, y_i) for i ∈ [n].
                byte[] qyBytes = qyMap.get(clientPid);
                byte[] dyBytes = serverSigmaOkvs.decode(serverSigmaOkvsStorage, yi);
                BytesUtils.xori(dyBytes, qyBytes);
                BigInteger dyBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(dyBytes);
                if (BigIntegerUtils.lessOrEqual(dyBigInteger, serverBigIntegerU) && BigIntegerUtils.greater(dyBigInteger, BigInteger.ONE)) {
                    // If 1 < d_i ≤ serverU, Bob set d_i = d_i
                    return dyBigInteger.intValue();
                } else {
                    // else, Bob set d_i = 1
                    return 1;
                }
            })
        );
    }

    private Map<ByteBuffer, T> generateClientPmidMap() {
        // 构建客户端PmidMap
        Map<ByteBuffer, T> clientPmidMap = new ConcurrentHashMap<>(clientSetSize * clientU);
        Stream<ByteBuffer> clientPidStream = clientPidMap.keySet().stream();
        clientPidStream = parallel ? clientPidStream.parallel() : clientPidStream;
        clientPidStream.forEach(clientPid -> {
            T y = clientPidMap.get(clientPid);
            byte[] clientPidBytes = clientPid.array();
            for (int j = 1; j <= clientElementMap.get(y) * dyMap.get(clientPid); j++) {
                byte[] extendClientPid = ByteBuffer.allocate(clientPidBytes.length + Integer.BYTES)
                    .put(clientPidBytes).put(IntUtils.intToByteArray(j))
                    .array();
                byte[] pmid = pmidMap.digestToBytes(extendClientPid);
                clientPmidMap.put(ByteBuffer.wrap(pmid), y);
            }
        });
        qyMap = null;
        clientPidMap = null;
        dyMap = null;
        return clientPmidMap;
    }

    private Set<ByteBuffer> union(Map<ByteBuffer, T> clientPmidMap) throws MpcAbortException {
        // 双方同步对方PSU的元素数量
        List<byte[]> clientPsuSetSizePayload = new LinkedList<>();
        clientPsuSetSizePayload.add(IntUtils.intToByteArray(clientPmidMap.size()));
        DataPacketHeader clientPsuSetSizeHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPsuSetSizeHeader, clientPsuSetSizePayload));

        DataPacketHeader serverPsuSetSizeHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPsuSetSizePayload = rpc.receive(serverPsuSetSizeHeader).getPayload();
        MpcAbortPreconditions.checkArgument(serverPsuSetSizePayload.size() == 1);
        int serverPsuSetSize = IntUtils.byteArrayToInt(serverPsuSetSizePayload.remove(0));
        // Alice and Bob invoke the PSU functionality F_{psu}. Bob acts as receiver with input ID_y and receives the union
        Set<ByteBuffer> pmidSet = psuClient.psu(clientPmidMap.keySet(), serverPsuSetSize, pmidByteLength);
        // Bob sends union
        List<byte[]> unionPayload = pmidSet.stream().map(ByteBuffer::array).collect(Collectors.toList());
        DataPacketHeader unionHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Zcl22SloppyPmidPtoDesc.PtoStep.CLIENT_SEND_UNION.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(unionHeader, unionPayload));
        return pmidSet;
    }
}
