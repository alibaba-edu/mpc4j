package edu.alibaba.mpc4j.s2pc.pso.psu.jsz22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
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
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfSender;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuClient;

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
        firstOsnReceiver.addLogLevel();
        oprfSender = OprfFactory.createOprfSender(clientRpc, serverParty, config.getOprfConfig());
        oprfSender.addLogLevel();
        secondOsnSender = OsnFactory.createSender(clientRpc, serverParty, config.getOsnConfig());
        secondOsnSender.addLogLevel();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        firstOsnReceiver.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
        oprfSender.setTaskId(taskIdPrf.getLong(2, taskIdBytes, Long.MAX_VALUE));
        secondOsnSender.setTaskId(taskIdPrf.getLong(3, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        firstOsnReceiver.setParallel(parallel);
        oprfSender.setParallel(parallel);
        secondOsnSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        firstOsnReceiver.addLogLevel();
        oprfSender.addLogLevel();
        secondOsnSender.addLogLevel();
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int maxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerElementSize);
        // 初始化各个子协议
        firstOsnReceiver.init(maxBinNum);
        oprfSender.init(maxBinNum);
        secondOsnSender.init(maxBinNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Set<ByteBuffer> psu(Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize, elementByteLength);
        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

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
            taskId, getPtoDesc().getPtoId(), Jsz22SfsPsuPtoDesc.PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        handleCuckooHashKeyPayload(cuckooHashKeyPayload);
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashTime);

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
        info("{}{} Server Step 2/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), osnTime);

        stopWatch.start();
        // S and R invoke the ideal functionality F_{mpOPRF}
        // R obtains the key k;
        oprfSenderOutput = oprfSender.oprf(binNum);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 3/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

        stopWatch.start();
        // For i ∈ [b], S yj ∈ Y_B[π(i)], R computes F(k, y_j ⊕ a′_i)
        List<byte[]> clientOprfPayload = generateClientOprfPayload();
        DataPacketHeader clientOprfHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Jsz22SfsPsuPtoDesc.PtoStep.CLIENT_SEND_OPRFS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientOprfHeader, clientOprfPayload));
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 4/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), checkTime);

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
        info("{}{} Server Step 5/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), secondOsnTime);

        stopWatch.start();
        DataPacketHeader zsHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Jsz22SfsPsuPtoDesc.PtoStep.SERVER_SEND_ZS.ordinal(), extraInfo,
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
        info("{}{} Client Step 6/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), unionTime);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
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
