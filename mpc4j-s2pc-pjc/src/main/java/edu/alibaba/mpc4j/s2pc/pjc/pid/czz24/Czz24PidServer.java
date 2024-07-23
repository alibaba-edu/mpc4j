package edu.alibaba.mpc4j.s2pc.pjc.pid.czz24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
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
import edu.alibaba.mpc4j.s2pc.opf.oprf.*;
import edu.alibaba.mpc4j.s2pc.pjc.pid.AbstractPidParty;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidUtils;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuServer;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * CZZ24 PID server
 *
 * @author Yufei Wang
 * @date 2023/7/27
 */
public class Czz24PidServer<T> extends AbstractPidParty<T> {
    /**
     * OPRF receiver
     */
    private final OprfReceiver oprfReceiver;
    /**
     * OPRF sender
     */
    private final OprfSender oprfSender;
    /**
     * PSU
     */
    private final PsuServer psuServer;
    /**
     * Sloppy OKVS type
     */
    private final Gf2eDokvsType sloppyOkvsType;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * cuckoo hash num
     */
    private final int cuckooHashNum;
    /**
     * server PID PRF key
     */
    private byte[] serverPidPrfKey;
    /**
     * server OKVS key
     */
    private byte[][] serverOkvsHashKeys;
    /**
     * client OKVS key
     */
    private byte[][] clientOkvsHashKeys;
    /**
     * PID byte length
     */
    private int pidByteLength;
    /**
     * PID map
     */
    private Hash pidMap;
    /**
     * server PID PRF
     */
    private Prf serverPidPrf;
    /**
     * server bin num
     */
    private int serverBinNum;
    /**
     * server cuckoo hash bin
     */
    private CuckooHashBin<T> serverCuckooHashBin;
    /**
     * (f_1^B, ..., f_m^B)
     */
    private OprfReceiverOutput kbOprfOutput;
    /**
     * client bin num
     */
    private int clientBinNum;
    /**
     * client cuckoo hashes
     */
    private Prf[] clientCuckooHashes;
    /**
     * (k_1^A, ..., k_m^A)
     */
    private OprfSenderOutput kaOprfKey;

    public Czz24PidServer(Rpc serverRpc, Party clientParty, Czz24PidConfig config) {
        super(Czz24PidPtoDesc.getInstance(), serverRpc, clientParty, config);
        oprfReceiver = OprfFactory.createOprfReceiver(serverRpc, clientParty, config.getOprfConfig());
        addSubPto(oprfReceiver);
        oprfSender = OprfFactory.createOprfSender(serverRpc, clientParty, config.getOprfConfig());
        addSubPto(oprfSender);
        psuServer = PsuFactory.createServer(serverRpc, clientParty, config.getPsuConfig());
        addSubPto(psuServer);
        sloppyOkvsType = config.getSloppyOkvsType();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxOwnElementSetSize, int maxOtherElementSetSize) throws MpcAbortException {
        setInitInput(maxOwnElementSetSize, maxOtherElementSetSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxServerBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxOwnElementSetSize);
        oprfReceiver.init(maxServerBinNum);
        int maxClientBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxOtherElementSetSize);
        oprfSender.init(maxClientBinNum);
        psuServer.init(maxOwnElementSetSize, maxOtherElementSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        stopWatch.start();
        //s^A为本地prf_key,其形式为与数据等长的随机字节向量
        serverPidPrfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(serverPidPrfKey);
        List<byte[]> serverKeysPayload = new LinkedList<>();
        // 服务端PID的OKVS密钥
        int sloppyOkvsHashKeyNum = Gf2eDokvsFactory.getHashKeyNum(sloppyOkvsType);
        serverOkvsHashKeys = IntStream.range(0, sloppyOkvsHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] okvsKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(okvsKey);
                serverKeysPayload.add(okvsKey);
                return okvsKey;
            })
            .toArray(byte[][]::new);
        DataPacketHeader serverKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Czz24PidPtoDesc.PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverKeysHeader, serverKeysPayload));
        // 接收客户端密钥
        DataPacketHeader clientKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Czz24PidPtoDesc.PtoStep.CLIENT_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientKeysPayload = rpc.receive(clientKeysHeader).getPayload();
        // 客户端PID的OKVS密钥
        MpcAbortPreconditions.checkArgument(clientKeysPayload.size() == sloppyOkvsHashKeyNum);
        clientOkvsHashKeys = clientKeysPayload.toArray(new byte[0][]);
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
        // The parties call F_{aOPRF}, where Alice is sender.
        // Alice receives output (k_1^A, ..., k_m^A)
        kaOprfKey = oprfSender.oprf(clientBinNum);
        // Alice sends OKVS
        List<byte[]> serverOkvsPayload = generateServerOkvsPayload();
        DataPacketHeader serverOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Czz24PidPtoDesc.PtoStep.SERVER_SEND_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverOkvsHeader, serverOkvsPayload));
        // Alice receives OKVS
        DataPacketHeader clientOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Czz24PidPtoDesc.PtoStep.CLIENT_SEND_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientOkvsPayload = rpc.receive(clientOkvsHeader).getPayload();
        Map<ByteBuffer, T> serverPidMap = handleClientOkvsPayload(clientOkvsPayload);
        stopWatch.stop();
        long serverPidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, serverPidMapTime);

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
            encodeTaskId, getPtoDesc().getPtoId(), Czz24PidPtoDesc.PtoStep.CLIENT_SEND_UNION.ordinal(), extraInfo,
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

    private void initVariable() throws MpcAbortException {
        // PID字节长度等于λ + log(n) + log(m) = λ + log(m * n)
        pidByteLength = PidUtils.getPidByteLength(ownElementSetSize, otherElementSetSize);
        pidMap = HashFactory.createInstance(envType, pidByteLength);
        serverPidPrf = PrfFactory.createInstance(envType, pidByteLength);
        serverPidPrf.setKey(serverPidPrfKey);
        // Alice inserts items into cuckoo hash
        List<byte[]> serverCuckooHashKeyPayload = generateServerCuckooHashKeyPayload();
        DataPacketHeader serverCuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Czz24PidPtoDesc.PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverCuckooHashKeyHeader, serverCuckooHashKeyPayload));
        // Bob inserts items into cuckoo hash
        DataPacketHeader clientCuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Czz24PidPtoDesc.PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientCuckooHashKeyPayload = rpc.receive(clientCuckooHashKeyHeader).getPayload();
        handleClientCuckooHashKeyPayload(clientCuckooHashKeyPayload);
    }

    private List<byte[]> generateServerCuckooHashKeyPayload() {
        serverBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, ownElementSetSize);
        serverCuckooHashBin = CuckooHashBinFactory.createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, ownElementSetSize, ownElementArrayList, secureRandom
        );
        serverCuckooHashBin.insertPaddingItems(secureRandom);
        return Arrays.stream(serverCuckooHashBin.getHashKeys()).collect(Collectors.toList());
    }

    private Map<ByteBuffer, T> handleClientOkvsPayload(List<byte[]> clientOkvsPayload) throws MpcAbortException {
        int clientOkvsM = Gf2eDokvsFactory.getM(envType, sloppyOkvsType, otherElementSetSize * cuckooHashNum);
        MpcAbortPreconditions.checkArgument(clientOkvsPayload.size() == clientOkvsM);
        byte[][] clientOkvsStorage = clientOkvsPayload.toArray(new byte[0][]);
        Gf2eDokvs<ByteBuffer> clientOkvs = Gf2eDokvsFactory.createInstance(
            envType, sloppyOkvsType, otherElementSetSize * cuckooHashNum, pidByteLength * Byte.SIZE, clientOkvsHashKeys
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
                byte[] pidBytes = clientOkvs.decode(clientOkvsStorage, pidExtendElementBytes);
                BytesUtils.xori(pidBytes, pidMap.digestToBytes(kbOprfOutput.getPrf(serverBinIndex)));
                BytesUtils.xori(pidBytes, serverPidPrf.getBytes(elementBytes));
                return ByteBuffer.wrap(pidBytes);
            })
            .toArray(ByteBuffer[]::new);
        Map<ByteBuffer, T> serverPidMap = new HashMap<>(ownElementSetSize);
        IntStream.range(0, serverBinNum).forEach(serverBinIndex -> {
            if (serverPids[serverBinIndex] != null) {
                serverPidMap.put(serverPids[serverBinIndex], serverCuckooHashBin.getHashBinEntry(serverBinIndex).getItem());
            }
        });
        serverCuckooHashBin = null;
        kbOprfOutput = null;
        return serverPidMap;
    }

    private void handleClientCuckooHashKeyPayload(List<byte[]> clientCuckooHashKeyPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientCuckooHashKeyPayload.size() == cuckooHashNum);
        clientBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, otherElementSetSize);
        clientCuckooHashes = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                byte[] key = clientCuckooHashKeyPayload.remove(0);
                Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
                hash.setKey(key);
                return hash;
            })
            .toArray(Prf[]::new);
    }

    private List<byte[]> generateServerOkvsPayload() {
        // 客户端字节元素
        ByteBuffer[] serverElementByteBuffers = ownElementArrayList.stream()
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
        ByteBuffer[] serverOkvsKeyArray = Arrays.stream(serverExtendElementByteBuffers)
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
        byte[][] serverOkvsValueArray = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                // value值涉及密码学操作，并发处理
                IntStream serverElementIntStream = IntStream.range(0, ownElementSetSize);
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
        Map<ByteBuffer, byte[]> serverOkvsKeyValueMap = new HashMap<>(ownElementSetSize * cuckooHashNum);
        IntStream.range(0, ownElementSetSize * cuckooHashNum).forEach(index ->
            serverOkvsKeyValueMap.put(serverOkvsKeyArray[index], serverOkvsValueArray[index])
        );
        Gf2eDokvs<ByteBuffer> serverOkvs = Gf2eDokvsFactory.createInstance(
            envType, sloppyOkvsType, ownElementSetSize * cuckooHashNum, pidByteLength * Byte.SIZE, serverOkvsHashKeys
        );
        // 编码可以并行处理
        serverOkvs.setParallelEncode(parallel);
        byte[][] serverOkvsStorage = serverOkvs.encode(serverOkvsKeyValueMap, false);
        kaOprfKey = null;
        return Arrays.stream(serverOkvsStorage).collect(Collectors.toList());
    }
}
