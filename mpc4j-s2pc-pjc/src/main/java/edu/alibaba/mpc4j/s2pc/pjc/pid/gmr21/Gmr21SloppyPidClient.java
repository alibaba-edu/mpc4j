package edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21;

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
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.*;
import edu.alibaba.mpc4j.s2pc.pjc.pid.AbstractPidParty;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21.Gmr21SloppyPidPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * GMR21宽松PID协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/5/12
 */
public class Gmr21SloppyPidClient<T> extends AbstractPidParty<T> {
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
     * 客户端OKVS密钥
     */
    private byte[][] clientOkvsHashKeys;
    /**
     * 服务端OKVS密钥
     */
    private byte[][] serverOkvsHashKeys;
    /**
     * PID字节长度
     */
    private int pidByteLength;
    /**
     * PID映射函数
     */
    private Hash pidMap;
    /**
     * 客户端PID伪随机函数
     */
    private Prf clientPidPrf;
    /**
     * 服务端桶数量
     */
    private int serverBinNum;
    /**
     * 服务端布谷鸟哈希
     */
    private Prf[] serverCuckooHashes;
    /**
     * (k_1^B, ..., k_n^B)
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
     * (f_1^A, ..., f_n^A)
     */
    private OprfReceiverOutput kaOprfOutput;

    public Gmr21SloppyPidClient(Rpc clientRpc, Party serverParty, Gmr21SloppyPidConfig config) {
        super(Gmr21SloppyPidPtoDesc.getInstance(), clientRpc, serverParty, config);
        oprfSender = OprfFactory.createOprfSender(clientRpc, serverParty, config.getOprfConfig());
        addSubPto(oprfSender);
        oprfReceiver = OprfFactory.createOprfReceiver(clientRpc, serverParty, config.getOprfConfig());
        addSubPto(oprfReceiver);
        psuClient = PsuFactory.createClient(clientRpc, serverParty, config.getPsuConfig());
        addSubPto(psuClient);
        sloppyOkvsType = config.getSloppyOkvsType();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxOwnElementSetSize, int maxOtherElementSetSize) throws MpcAbortException {
        setInitInput(maxOwnElementSetSize, maxOtherElementSetSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxServerBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxOtherElementSetSize);
        oprfSender.init(maxServerBinNum);
        int maxClientBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxOwnElementSetSize);
        oprfReceiver.init(maxClientBinNum);
        psuClient.init(maxOwnElementSetSize, maxOtherElementSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        stopWatch.start();
        // s^B
        clientPidPrfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(clientPidPrfKey);
        List<byte[]> clientKeysPayload = new LinkedList<>();
        // 客户端PID的OKVS密钥
        int sloppyOkvsHashKeyNum = Gf2eDokvsFactory.getHashKeyNum(sloppyOkvsType);
        clientOkvsHashKeys = IntStream.range(0, sloppyOkvsHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] okvsKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(okvsKey);
                clientKeysPayload.add(okvsKey);
                return okvsKey;
            })
            .toArray(byte[][]::new);
        DataPacketHeader clientKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientKeysHeader, clientKeysPayload));
        // 接收服务端密钥
        DataPacketHeader serverKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverKeysPayload = rpc.receive(serverKeysHeader).getPayload();
        MpcAbortPreconditions.checkArgument(serverKeysPayload.size() == sloppyOkvsHashKeyNum);
        // 服务端PID的OKVS密钥
        serverOkvsHashKeys = serverKeysPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, keyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PidPartyOutput<T> pid(Set<T> ownElementSet, int otherElementSetSize) throws MpcAbortException {
        setPtoInput(ownElementSet, otherElementSetSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        initVariable();
        stopWatch.stop();
        long initVariableTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, initVariableTime);

        stopWatch.start();
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
        // Bob sends OKVS
        List<byte[]> clientOkvsPayload = generateClientOkvsPayload();
        DataPacketHeader clientOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientOkvsHeader, clientOkvsPayload));
        // Bob receives OKVS
        DataPacketHeader serverOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverOkvsPayload = rpc.receive(serverOkvsHeader).getPayload();
        Map<ByteBuffer, T> clientPidMap = handleServerOkvsPayload(serverOkvsPayload);
        stopWatch.stop();
        long clientPidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, clientPidMapTime);

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

    private void initVariable() throws MpcAbortException {
        pidByteLength = PidUtils.getPidByteLength(ownElementSetSize, otherElementSetSize);
        pidMap = HashFactory.createInstance(envType, pidByteLength);
        clientPidPrf = PrfFactory.createInstance(envType, pidByteLength);
        clientPidPrf.setKey(clientPidPrfKey);
        // Bob inserts items into cuckoo hash
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
        handleServerCuckooHashKeyPayload(serverCuckooHashKeyPayload);
    }

    private void handleServerCuckooHashKeyPayload(List<byte[]> serverCuckooHashKeyPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(serverCuckooHashKeyPayload.size() == cuckooHashNum);
        serverBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, otherElementSetSize);
        serverCuckooHashes = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                byte[] key = serverCuckooHashKeyPayload.remove(0);
                Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
                hash.setKey(key);
                return hash;
            })
            .toArray(Prf[]::new);
    }

    private List<byte[]> generateClientOkvsPayload() {
        // 客户端字节元素
        ByteBuffer[] clientElementByteBuffers = ownElementArrayList.stream()
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
        ByteBuffer[] clientOkvsKeyArray = Arrays.stream(clientExtendElementByteBuffers)
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
        byte[][] clientOkvsValueArray = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                // value值涉及密码学操作，并发处理
                IntStream clientElementIntStream = IntStream.range(0, ownElementSetSize);
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
        Map<ByteBuffer, byte[]> clientOkvsKeyValueMap = IntStream.range(0, ownElementSetSize * cuckooHashNum)
            .boxed()
            .collect(Collectors.toMap(index -> clientOkvsKeyArray[index], index -> clientOkvsValueArray[index]));
        Gf2eDokvs<ByteBuffer> clientOkvs = Gf2eDokvsFactory.createInstance(
            envType, sloppyOkvsType, ownElementSetSize * cuckooHashNum, pidByteLength * Byte.SIZE, clientOkvsHashKeys
        );
        // 编码可以并行处理
        clientOkvs.setParallelEncode(parallel);
        byte[][] clientOkvsStorage = clientOkvs.encode(clientOkvsKeyValueMap, false);
        kbOprfKey = null;
        return Arrays.stream(clientOkvsStorage).collect(Collectors.toList());
    }

    private List<byte[]> generateClientCuckooHashKeyPayload() {
        clientBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, ownElementSetSize);
        clientCuckooHashBin = CuckooHashBinFactory.createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, ownElementSetSize, ownElementArrayList, secureRandom
        );
        clientCuckooHashBin.insertPaddingItems(secureRandom);
        return Arrays.stream(clientCuckooHashBin.getHashKeys()).collect(Collectors.toList());
    }

    private Map<ByteBuffer, T> handleServerOkvsPayload(List<byte[]> serverOkvsPayload) throws MpcAbortException {
        int serverOkvsM = Gf2eDokvsFactory.getM(envType, sloppyOkvsType, otherElementSetSize * cuckooHashNum);
        MpcAbortPreconditions.checkArgument(serverOkvsPayload.size() == serverOkvsM);
        byte[][] serverOkvsStorage = serverOkvsPayload.toArray(new byte[0][]);
        Gf2eDokvs<ByteBuffer> serverOkvs = Gf2eDokvsFactory.createInstance(
            envType, sloppyOkvsType, otherElementSetSize * cuckooHashNum, pidByteLength * Byte.SIZE, serverOkvsHashKeys
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
}
