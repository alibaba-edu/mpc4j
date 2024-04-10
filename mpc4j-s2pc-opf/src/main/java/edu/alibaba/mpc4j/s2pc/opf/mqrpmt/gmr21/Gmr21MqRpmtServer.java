package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.AbstractMqRpmtServer;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.oprf.*;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GMR21-mqRPMT协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
public class Gmr21MqRpmtServer extends AbstractMqRpmtServer {
    /**
     * 布谷鸟哈希所用OPRF接收方
     */
    private final OprfReceiver cuckooHashOprfReceiver;
    /**
     * OSN接收方
     */
    private final OsnReceiver osnReceiver;
    /**
     * PEQT协议所用OPRF发送方
     */
    private final OprfSender peqtOprfSender;
    /**
     * OKVS类型
     */
    private final Gf2eDokvsType okvsType;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * 布谷鸟哈希函数数量
     */
    private final int cuckooHashNum;
    /**
     * 多项式有限域哈希
     */
    private Hash finiteFieldHash;
    /**
     * DOKVS hash keys
     */
    private byte[][] okvsHashKeys;
    /**
     * 无贮存区布谷鸟哈希
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;
    /**
     * 桶数量
     */
    private int binNum;
    /**
     * m for DOKVS
     */
    private int okvsM;
    /**
     * 交换映射
     */
    private int[] permutationMap;
    /**
     * 扩展元素字节
     */
    private byte[][] extendEntryBytes;
    /**
     * f_1, ..., f_m
     */
    private byte[][] fArray;
    /**
     * 向量(t_1, ..., t_m)
     */
    private Vector<byte[]> tVector;
    /**
     * a'_1, ..., a'_m
     */
    private byte[][] aPrimeArray;

    public Gmr21MqRpmtServer(Rpc serverRpc, Party clientParty, Gmr21MqRpmtConfig config) {
        super(Gmr21MqRpmtPtoDesc.getInstance(), serverRpc, clientParty, config);
        cuckooHashOprfReceiver = OprfFactory.createOprfReceiver(serverRpc, clientParty, config.getCuckooHashOprfConfig());
        addSubPto(cuckooHashOprfReceiver);
        osnReceiver = OsnFactory.createReceiver(serverRpc, clientParty, config.getOsnConfig());
        addSubPto(osnReceiver);
        peqtOprfSender = OprfFactory.createOprfSender(serverRpc, clientParty, config.getPeqtOprfConfig());
        addSubPto(peqtOprfSender);
        okvsType = config.getOkvsType();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerElementSize);
        int maxPrfNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType) * maxClientElementSize;
        // 初始化各个子协议
        cuckooHashOprfReceiver.init(maxBinNum, maxPrfNum);
        osnReceiver.init(maxBinNum);
        peqtOprfSender.init(maxBinNum);
        // 初始化多项式有限域哈希，根据论文实现，固定为64比特
        finiteFieldHash = HashFactory.createInstance(envType, Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        stopWatch.start();
        // 初始化OKVS密钥
        int okvsHashKeyNum = Gf2eDokvsFactory.getHashKeyNum(okvsType);
        okvsHashKeys = CommonUtils.generateRandomKeys(okvsHashKeyNum, secureRandom);
        List<byte[]> keysPayload = Arrays.stream(okvsHashKeys).collect(Collectors.toList());
        DataPacketHeader keysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keysHeader, keysPayload));
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, keyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public ByteBuffer[] mqRpmt(Set<ByteBuffer> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> cuckooHashKeyPayload = generateCuckooHashKeyPayload();
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverElementSize);
        // 设置OKVS大小
        okvsM = Gf2eDokvsFactory.getM(envType, okvsType, clientElementSize * cuckooHashNum);
        // 初始化PEQT哈希
        Hash peqtHash = HashFactory.createInstance(envType, Gmr21MqRpmtPtoDesc.getPeqtByteLength(binNum));
        // 构造交换映射
        List<Integer> shufflePermutationList = IntStream.range(0, binNum)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(shufflePermutationList, secureRandom);
        permutationMap = shufflePermutationList.stream().mapToInt(permutation -> permutation).toArray();
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, cuckooHashTime, "Server generates cuckoo hash keys and data structure");

        stopWatch.start();
        // 生成服务端元素输入列表，即哈希桶中的元素 = 原始元素 || hashindex，贮存区中的元素 = 原始元素
        generateCuckooHashOprfInput();
        OprfReceiverOutput cuckooHashOprfReceiverOutput = cuckooHashOprfReceiver.oprf(extendEntryBytes);
        IntStream oprfIntStream = IntStream.range(0, binNum);
        oprfIntStream = parallel ? oprfIntStream.parallel() : oprfIntStream;
        fArray = oprfIntStream
            .mapToObj(cuckooHashOprfReceiverOutput::getPrf)
            .map(finiteFieldHash::digestToBytes)
            .toArray(byte[][]::new);
        stopWatch.stop();
        long cuckooHashOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, cuckooHashOprfTime, "Server runs OPRF for cuckoo hash bins");

        DataPacketHeader okvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsPayload = rpc.receive(okvsHeader).getPayload();

        stopWatch.start();
        handleOkvsPayload(okvsPayload);
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, okvsTime, "Server handles OKVS");

        stopWatch.start();
        OsnPartyOutput osnReceiverOutput = osnReceiver.osn(permutationMap, Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH);
        handleOsnReceiverOutput(osnReceiverOutput);
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, osnTime, "Server runs OSN");

        stopWatch.start();
        OprfSenderOutput peqtOprfSenderOutput = peqtOprfSender.oprf(binNum);
        IntStream aPrimeOprfIntStream = IntStream.range(0, binNum);
        aPrimeOprfIntStream = parallel ? aPrimeOprfIntStream.parallel() : aPrimeOprfIntStream;
        List<byte[]> aPrimeOprfPayload = aPrimeOprfIntStream
            .mapToObj(aPrimeIndex -> peqtOprfSenderOutput.getPrf(aPrimeIndex, aPrimeArray[aPrimeIndex]))
            .map(peqtHash::digestToBytes)
            .collect(Collectors.toList());
        DataPacketHeader aPrimeOprfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_A_PRIME_OPRFS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(aPrimeOprfHeader, aPrimeOprfPayload));
        ByteBuffer[] serverVector = generateServerVector();
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, peqtTime, "Server runs OPRF for PEQT");

        logPhaseInfo(PtoState.PTO_END);
        return serverVector;
    }

    private List<byte[]> generateCuckooHashKeyPayload() {
        cuckooHashBin = CuckooHashBinFactory.createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, serverElementSize, serverElementArrayList, secureRandom
        );
        cuckooHashBin.insertPaddingItems(BOT_ELEMENT_BYTE_BUFFER);
        return Arrays.stream(cuckooHashBin.getHashKeys()).collect(Collectors.toList());
    }

    private void generateCuckooHashOprfInput() {
        extendEntryBytes = IntStream.range(0, binNum)
            .mapToObj(binIndex -> {
                HashBinEntry<ByteBuffer> hashBinEntry = cuckooHashBin.getHashBinEntry(binIndex);
                int hashIndex = hashBinEntry.getHashIndex();
                byte[] entryBytes = hashBinEntry.getItemByteArray();
                ByteBuffer extendEntryByteBuffer = ByteBuffer.allocate(entryBytes.length + Integer.BYTES);
                // x || i
                extendEntryByteBuffer.put(entryBytes);
                extendEntryByteBuffer.putInt(hashIndex);
                return extendEntryByteBuffer.array();
            })
            .toArray(byte[][]::new);
    }

    private void handleOkvsPayload(List<byte[]> okvsPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(okvsPayload.size() == okvsM);
        byte[][] storage = okvsPayload.toArray(new byte[0][]);
        Gf2eDokvs<ByteBuffer> okvs = Gf2eDokvsFactory.createInstance(
            envType, okvsType, clientElementSize * cuckooHashNum,
            Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH * Byte.SIZE, okvsHashKeys
        );
        IntStream okvsDecodeIntStream = IntStream.range(0, binNum);
        okvsDecodeIntStream = parallel ? okvsDecodeIntStream.parallel() : okvsDecodeIntStream;
        tVector = okvsDecodeIntStream
            .mapToObj(index -> {
                // 扩展输入
                ByteBuffer key = ByteBuffer.wrap(extendEntryBytes[index]);
                byte[] pi = okvs.decode(storage, key);
                byte[] fi = fArray[index];
                BytesUtils.xori(pi, fi);
                return pi;
            })
            .collect(Collectors.toCollection(Vector::new));
        fArray = null;
        extendEntryBytes = null;
    }

    private void handleOsnReceiverOutput(OsnPartyOutput osnReceiverOutput) {
        Vector<byte[]> tPiVector = PermutationNetworkUtils.permutation(permutationMap, tVector);
        tVector = null;
        aPrimeArray = IntStream.range(0, binNum)
            .mapToObj(index -> {
                byte[] ai = osnReceiverOutput.getShare(index);
                byte[] ti = tPiVector.elementAt(index);
                BytesUtils.xori(ai, ti);
                return ai;
            })
            .toArray(byte[][]::new);
    }

    private ByteBuffer[] generateServerVector() {
        // 求并集的时候服务端发给客户端的是交换后的数据顺序
        Vector<Integer> permutedIndexVector = IntStream.range(0, binNum).boxed()
            .collect(Collectors.toCollection(Vector::new));
        permutedIndexVector = PermutationNetworkUtils.permutation(permutationMap, permutedIndexVector);
        int[] permutedIndexArray = permutedIndexVector.stream().mapToInt(permutedIndex -> permutedIndex).toArray();
        IntStream binIndexIntStream = IntStream.range(0, binNum);
        binIndexIntStream = parallel ? binIndexIntStream.parallel() : binIndexIntStream;
        ByteBuffer[] serverVector = binIndexIntStream
            .mapToObj(index -> {
                int permuteIndex = permutedIndexArray[index];
                return cuckooHashBin.getHashBinEntry(permuteIndex).getItem();
            })
            .toArray(ByteBuffer[]::new);
        permutationMap = null;
        cuckooHashBin = null;
        return serverVector;
    }
}
