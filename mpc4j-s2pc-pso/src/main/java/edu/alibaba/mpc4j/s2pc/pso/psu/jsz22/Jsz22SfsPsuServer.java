package edu.alibaba.mpc4j.s2pc.pso.psu.jsz22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnPartyOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.*;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractOoPsuServer;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfsPsuPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * JSZ22-SFS-PSU协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/03/22
 */
public class Jsz22SfsPsuServer extends AbstractOoPsuServer {
    /**
     * 第一轮OSN发送方
     */
    private final DosnSender firstDosnSender;
    /**
     * 第二轮OSN接收方
     */
    private final DosnReceiver secondDosnReceiver;
    /**
     * 第一轮OSN发送方
     */
    private final RosnSender firstRosnSender;
    /**
     * 第二轮OSN接收方
     */
    private final RosnReceiver secondRosnReceiver;
    /**
     * OPRF接收方
     */
    private final OprfReceiver oprfReceiver;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * 布谷鸟哈希
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;
    /**
     * 桶数量
     */
    private int binNum;
    /**
     * 最大桶大小
     */
    private int maxBinSize;
    /**
     * OPRF输出映射
     */
    private Hash oprfOutputMap;
    /**
     * π′
     */
    private int[] secondPi;
    /**
     * {a_1, ..., a_b}
     */
    private byte[][] aArray;
    /**
     * 服务端OPRF集合
     */
    private Set<ByteBuffer> serverOprfSet;
    /**
     * U_i
     */
    private boolean[] uArray;
    /**
     * π′的OSN协议输出
     */
    private DosnPartyOutput secondOsnReceiverOutput;
    /**
     * first rosn result
     */
    RosnSenderOutput firstRosnSenderOutput;
    /**
     * second rosn result
     */
    RosnReceiverOutput secondRosnReceiverOutput;

    public Jsz22SfsPsuServer(Rpc serverRpc, Party clientParty, Jsz22SfsPsuConfig config) {
        super(Jsz22SfsPsuPtoDesc.getInstance(), serverRpc, clientParty, config);
        firstDosnSender = DosnFactory.createSender(serverRpc, clientParty, config.getOsnConfig());
        addSubPto(firstDosnSender);
        secondDosnReceiver = DosnFactory.createReceiver(serverRpc, clientParty, config.getOsnConfig());
        addSubPto(secondDosnReceiver);
        firstRosnSender = RosnFactory.createSender(serverRpc, clientParty, config.getRosnConfig());
        addSubPto(firstRosnSender);
        secondRosnReceiver = RosnFactory.createReceiver(serverRpc, clientParty, config.getRosnConfig());
        addSubPto(secondRosnReceiver);
        oprfReceiver = OprfFactory.createOprfReceiver(serverRpc, clientParty, config.getOprfConfig());
        addSubPto(oprfReceiver);
        cuckooHashBinType = config.getCuckooHashBinType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerElementSize);
        // 初始化各个子协议
        firstDosnSender.init();
        secondDosnReceiver.init();
        firstRosnSender.init();
        secondRosnReceiver.init();
        oprfReceiver.init(maxBinNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void preCompute(int serverElementSize, int clientElementSize, int elementByteLength) throws MpcAbortException {
        checkPrecomputeInput(serverElementSize, clientElementSize, elementByteLength);

        logPhaseInfo(PtoState.PTO_BEGIN, "Pre-computation");
        stopWatch.start();
        int precomputeBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
        int[] precomputeSecondPi = PermutationNetworkUtils.randomPermutation(precomputeBinNum, secureRandom);
        firstRosnSenderOutput = firstRosnSender.rosn(precomputeBinNum, elementByteLength);
        secondRosnReceiverOutput = secondRosnReceiver.rosn(precomputeSecondPi, elementByteLength);
        stopWatch.stop();
        long precomputeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, precomputeTime);

        logPhaseInfo(PtoState.PTO_END, "Pre-computation");

    }

    @Override
    public void psu(Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize, elementByteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverElementSize);
        if (validSecondPrecomputation()) {
            secondPi = IntUtils.clone(secondRosnReceiverOutput.getPi());
        } else {
            secondPi = PermutationNetworkUtils.randomPermutation(binNum, secureRandom);
        }
        maxBinSize = MaxBinSizeUtils.expectMaxBinSize(clientElementSize, binNum);
        // 初始化OPRF哈希
        int oprfOutputByteLength = Jsz22SfsPsuPtoDesc.getOprfByteLength(binNum, maxBinSize);
        oprfOutputMap = HashFactory.createInstance(envType, oprfOutputByteLength);
        // S inserts set X into the Cuckoo hash table, and fills empty bins with the dummy item d
        List<byte[]> cuckooHashKeyPayload = generateCuckooHashKeyPayload();
        sendOtherPartyPayload(PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), cuckooHashKeyPayload);

        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 6, cuckooHashTime);

        stopWatch.start();
        // 构建服务端元素向量(x_1, ..., x_m)
        byte[][] xVector = IntStream.range(0, binNum)
            .mapToObj(binIndex -> cuckooHashBin.getHashBinEntry(binIndex).getItemByteArray())
            .toArray(byte[][]::new);
        cuckooHashBin = null;
        // S and R invoke the ideal functionality F_{PS}.
        // S acts as P_0 with input set X_S, obtains the shuffled shares {a_1, a_2, ... , a_b}.
        DosnPartyOutput firstOsnSenderOutput;
        if (validFirstPrecomputation()) {
            firstOsnSenderOutput = firstDosnSender.dosn(xVector, elementByteLength, firstRosnSenderOutput);
            firstRosnSenderOutput = null;
        } else {
            firstOsnSenderOutput = firstDosnSender.dosn(xVector, elementByteLength);
        }
        aArray = IntStream.range(0, binNum)
            .mapToObj(firstOsnSenderOutput::getShare)
            .toArray(byte[][]::new);
        stopWatch.stop();
        long firstOsnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 6, firstOsnTime);

        stopWatch.start();
        // S and R invoke the ideal functionality F_{mpOPRF}
        // S acts as P_0 with her shuffled shares {a_i}_{i ∈ [b]}, and obtains the outputs {F(k, a_i)}_{i ∈ [b]};
        OprfReceiverOutput oprfReceiverOutput = oprfReceiver.oprf(aArray);
        IntStream binIndexIntStream = parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        serverOprfSet = binIndexIntStream
            .mapToObj(oprfReceiverOutput::getPrf)
            .map(oprf -> oprfOutputMap.digestToBytes(oprf))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 6, oprfTime);

        stopWatch.start();
        // S checks if F(k, a_i) is in I_i, if not, S sets U[i] = 1, otherwise, sets U[i] = 0;
        List<byte[]> clientOprfPayload = receiveOtherPartyEqualSizePayload(
            PtoStep.CLIENT_SEND_OPRFS.ordinal(), binNum * maxBinSize, oprfOutputByteLength);
        handleClientOprfPayload(clientOprfPayload);
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 6, checkTime);

        stopWatch.start();
        // S and R invoke the ideal functionality F_{PS}.
        // S acts as P_1 with a random permutation π′, S obtains the shuffled share sets {s^1_1, s^1_2, ..., s^1_b}
        if (validSecondPrecomputation()) {
            secondOsnReceiverOutput = secondDosnReceiver.dosn(secondPi, elementByteLength, secondRosnReceiverOutput);
            secondRosnReceiverOutput = null;
        } else {
            secondOsnReceiverOutput = secondDosnReceiver.dosn(secondPi, elementByteLength);
        }
        stopWatch.stop();
        long secondOsnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 6, secondOsnTime);

        stopWatch.start();
        // For i ∈ [b]: If U[π′(i)] = 1, S sets z_i = a_{π′(i)} ⊕ s^1_i , otherwise, sets z_i = ⊥, then sends z_i to R;
        List<byte[]> zsPayload = generateZsPayload();
        sendOtherPartyPayload(PtoStep.SERVER_SEND_ZS.ordinal(), zsPayload);
        stopWatch.stop();
        long zsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 6, 6, zsTime);

        logPhaseInfo(PtoState.PTO_END);
    }

    private boolean validFirstPrecomputation() {
        return firstRosnSenderOutput != null
            && firstRosnSenderOutput.getNum() == binNum
            && firstRosnSenderOutput.getByteLength() == elementByteLength;
    }

    private boolean validSecondPrecomputation() {
        return secondRosnReceiverOutput != null
            && secondRosnReceiverOutput.getNum() == binNum
            && secondRosnReceiverOutput.getByteLength() == elementByteLength;
    }

    private List<byte[]> generateCuckooHashKeyPayload() {
        cuckooHashBin = CuckooHashBinFactory.createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, serverElementSize, serverElementArrayList, secureRandom
        );
        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
        return Arrays.stream(cuckooHashBin.getHashKeys()).collect(Collectors.toList());
    }

    private void handleClientOprfPayload(List<byte[]> clientOprfPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientOprfPayload.size() == binNum * maxBinSize);
        byte[][] clientFlattenOprfs = clientOprfPayload.toArray(new byte[0][]);
        IntStream binIndexStream = parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        // 这里与论文描述相反，能匹配上则设置为true
        uArray = new boolean[binNum];
        binIndexStream.forEach(binIndex -> {
            for (int itemIndex = 0; itemIndex < maxBinSize; itemIndex++) {
                if (serverOprfSet.contains(ByteBuffer.wrap(clientFlattenOprfs[binIndex * maxBinSize + itemIndex]))) {
                    uArray[binIndex] = true;
                    break;
                }
            }
        });
        serverOprfSet = null;
    }

    private List<byte[]> generateZsPayload() {
        IntStream binIndexStream = parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        List<byte[]> zsPayload = binIndexStream
            .mapToObj(binIndex -> {
                // 这里与论文描述相反，uArray代表能匹配上
                if (!uArray[secondPi[binIndex]]) {
                    return BytesUtils.xor(aArray[secondPi[binIndex]], secondOsnReceiverOutput.getShare(binIndex));
                } else {
                    return new byte[0];
                }
            })
            .collect(Collectors.toList());
        uArray = null;
        aArray = null;
        secondOsnReceiverOutput = null;
        secondPi = null;

        return zsPayload;
    }
}
