package edu.alibaba.mpc4j.s2pc.pso.psu.jsz22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.EmptyPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfsPsuPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * JSZ22-SFS-PSU协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/03/22
 */
public class Jsz22SfsPsuClient extends AbstractPsuClient {
    /**
     * 第一轮OSN接收方
     */
    private final OsnReceiver firstOsnReceiver;
    /**
     * OPRF发送方
     */
    private final OprfSender oprfSender;
    /**
     * 第二轮OSN发送方
     */
    private final OsnSender secondOsnSender;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * 布谷鸟哈希函数数量
     */
    private final int cuckooHashNum;
    /**
     * OPRF输出字节长度
     */
    private int oprfOutputByteLength;
    /**
     * OPRF输出映射
     */
    private Hash oprfOutputMap;
    /**
     * π
     */
    private int[] firstPi;
    /**
     * 桶数量
     */
    private int binNum;
    /**
     * 最大桶大小
     */
    private int maxBinSize;
    /**
     * 简单哈希桶
     */
    private EmptyPadHashBin<ByteBuffer> hashBin;
    /**
     * a'_1, ..., a'_m
     */
    private byte[][] aPrimeArray;
    /**
     * OPRF发送方输出
     */
    OprfSenderOutput oprfSenderOutput;

    public Jsz22SfsPsuClient(Rpc clientRpc, Party serverParty, Jsz22SfsPsuConfig config) {
        super(Jsz22SfsPsuPtoDesc.getInstance(), clientRpc, serverParty, config);
        firstOsnReceiver = OsnFactory.createReceiver(clientRpc, serverParty, config.getOsnConfig());
        addSubPto(firstOsnReceiver);
        oprfSender = OprfFactory.createOprfSender(clientRpc, serverParty, config.getOprfConfig());
        addSubPto(oprfSender);
        secondOsnSender = OsnFactory.createSender(clientRpc, serverParty, config.getOsnConfig());
        addSubPto(secondOsnSender);
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerElementSize);
        // 初始化各个子协议
        firstOsnReceiver.init(maxBinNum);
        oprfSender.init(maxBinNum);
        secondOsnSender.init(maxBinNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<ByteBuffer> psu(Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize, elementByteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 初始化OPRF哈希
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverElementSize);
        maxBinSize = MaxBinSizeUtils.expectMaxBinSize(clientElementSize, binNum);
        oprfOutputByteLength = Jsz22SfsPsuPtoDesc.getOprfByteLength(binNum, maxBinSize);
        oprfOutputMap = HashFactory.createInstance(getEnvType(), oprfOutputByteLength);
        // 构造交换映射
        List<Integer> firstPiList = IntStream.range(0, binNum)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(firstPiList, secureRandom);
        firstPi = firstPiList.stream().mapToInt(permutation -> permutation).toArray();
        // 设置布谷鸟哈希
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        handleCuckooHashKeyPayload(cuckooHashKeyPayload);
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 6, cuckooHashTime);

        stopWatch.start();
        // S and R invoke the ideal functionality F_{PS}.
        // R acts as P_1 with a permutation π, obtains the shuffled shares {a'_1, a'_2, ... , a'_b}.
        OsnPartyOutput firstOsnReceiverOutput = firstOsnReceiver.osn(firstPi, elementByteLength);
        aPrimeArray = IntStream.range(0, binNum)
            .mapToObj(firstOsnReceiverOutput::getShare)
            .toArray(byte[][]::new);
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 6, osnTime);

        stopWatch.start();
        // S and R invoke the ideal functionality F_{mpOPRF}
        // R obtains the key k;
        oprfSenderOutput = oprfSender.oprf(binNum);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 6, oprfTime);

        stopWatch.start();
        // For i ∈ [b], S yj ∈ Y_B[π(i)], R computes F(k, y_j ⊕ a′_i)
        List<byte[]> clientOprfPayload = generateClientOprfPayload();
        DataPacketHeader clientOprfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OPRFS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientOprfHeader, clientOprfPayload));
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 6, checkTime);

        stopWatch.start();
        // S and R invoke the ideal functionality F_{PS}.
        // R acts as P_0 with input set {a′_i}_{i ∈ [b]}, obtains the shuffled share sets {s^2_1, s^2_2, ..., s^2_b}
        Vector<byte[]> aPrimeVector = Arrays.stream(aPrimeArray).collect(Collectors.toCollection(Vector::new));
        aPrimeArray = null;
        OsnPartyOutput secondOsnSenderOutput = secondOsnSender.osn(aPrimeVector, elementByteLength);
        byte[][] s2Array = IntStream.range(0, binNum)
            .mapToObj(secondOsnSenderOutput::getShare)
            .toArray(byte[][]::new);
        stopWatch.stop();
        long secondOsnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 6, secondOsnTime);

        stopWatch.start();
        DataPacketHeader zsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ZS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> zsPayload = rpc.receive(zsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(zsPayload.size() == binNum);
        byte[][] zsArray = zsPayload.toArray(new byte[0][]);
        // If z_i  ̸= ⊥ and z_i ⊕ s^2_i  ̸= d, R sets Z = Z ∪ {zi ⊕ s^2_i}
        Set<ByteBuffer> union = IntStream.range(0, binNum)
            .mapToObj(binIndex -> {
                byte[] zi = zsArray[binIndex];
                byte[] s2i = s2Array[binIndex];
                return zi.length == 0 ? botElementByteBuffer : ByteBuffer.wrap(BytesUtils.xor(zi, s2i));
            })
            .collect(Collectors.toSet());
        union.addAll(clientElementSet);
        union.remove(botElementByteBuffer);
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 6, 6, unionTime);

        logPhaseInfo(PtoState.PTO_END);
        return union;
    }

    private void handleCuckooHashKeyPayload(List<byte[]> cuckooHashKeyPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(cuckooHashKeyPayload.size() == cuckooHashNum);
        byte[][] cuckooHashKeys = cuckooHashKeyPayload.toArray(new byte[0][]);
        // R inserts set Y into the simple hash table
        hashBin = new EmptyPadHashBin<>(envType, binNum, clientElementSize, cuckooHashKeys);
        hashBin.insertItems(clientElementArrayList);
    }

    private List<byte[]> generateClientOprfPayload() {
        IntStream binIndexIntStream = IntStream.range(0, binNum);
        binIndexIntStream = parallel ? binIndexIntStream.parallel() : binIndexIntStream;
        List<byte[]> clientOprfPayload = binIndexIntStream
            .mapToObj(binIndex -> {
                // For each y_j ∈ Y_B[π(i)], R adds F(k, y_j ⊕ a′_i) to I_i
                ArrayList<ByteBuffer> bin = hashBin.getBin(firstPi[binIndex]).stream()
                    .map(HashBinEntry::getItem)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
                byte[][] oprfs = new byte[maxBinSize][oprfOutputByteLength];
                for (int index = 0; index < bin.size(); index++) {
                    // F(k, y_j ⊕ a′_i)
                    byte[] input = BytesUtils.xor(aPrimeArray[binIndex], bin.get(index).array());
                    oprfs[index] = oprfOutputMap.digestToBytes(oprfSenderOutput.getPrf(binIndex, input));
                }
                // r ← {0, 1}^{l_2}
                for (int index = bin.size(); index < maxBinSize; index++) {
                    secureRandom.nextBytes(oprfs[index]);
                }
                return oprfs;
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        hashBin = null;
        firstPi = null;
        oprfSenderOutput = null;

        return clientOprfPayload;
    }
}
