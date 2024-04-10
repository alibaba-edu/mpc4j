package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.SimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntNoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.AbstractMspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20.Ywl20UniMspCotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotReceiverOutput;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * YWL20-UNI-MSP-COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
public class Ywl20UniMspCotReceiver extends AbstractMspCotReceiver {
    /**
     * hash num
     */
    private static final int HASH_NUM = IntCuckooHashBinFactory.getHashNum(Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE);
    /**
     * BSP-COT receiver
     */
    private final BspCotReceiver bspCotReceiver;
    /**
     * pre-computed COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * α array
     */
    private int[] alphaArray;
    /**
     * cuckoo hash bin
     */
    private IntNoStashCuckooHashBin intNoStashCuckooHashBin;
    /**
     * hash bin
     */
    private SimpleIntHashBin intHashBin;
    /**
     * position maps
     */
    private ArrayList<TIntIntMap> positionMaps;

    public Ywl20UniMspCotReceiver(Rpc senderRpc, Party receiverParty, Ywl20UniMspCotConfig config) {
        super(Ywl20UniMspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bspCotReceiver = BspCotFactory.createReceiver(senderRpc, receiverParty, config.getBspCotConfig());
        addSubPto(bspCotReceiver);
    }

    @Override
    public void init(int maxT, int maxNum) throws MpcAbortException {
        setInitInput(maxT, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxBinNum = IntCuckooHashBinFactory.getBinNum(Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE, maxT);
        // In theory, eachNum = maxBinSize. but for larger T, minSize can be small. So we set maxEachNum = HASH_NUM * maxNum
        bspCotReceiver.init(maxBinNum, HASH_NUM * (maxNum + 1));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MspCotReceiverOutput receive(int t, int num) throws MpcAbortException {
        setPtoInput(t, num);
        return receive();
    }

    @Override
    public MspCotReceiverOutput receive(int t, int num, CotReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(t, num, preReceiverOutput);
        cotReceiverOutput = preReceiverOutput;
        return receive();
    }

    private MspCotReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> hashKeysPayload = generateHashKeysPayload();
        DataPacketHeader hashKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hashKeysHeader, hashKeysPayload));
        int[] targetArray = generateTargetArray();
        stopWatch.stop();
        long cuckooHashBinTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cuckooHashBinTime);

        stopWatch.start();
        // R sends (sp-extend, |B_j| + 1, p_j) to F_{SPCOT}
        BspCotReceiverOutput bspCotReceiverOutput;
        if (cotReceiverOutput == null) {
            bspCotReceiverOutput = bspCotReceiver.receive(targetArray, intHashBin.maxBinSize() + 1);
        } else {
            bspCotReceiverOutput = bspCotReceiver.receive(targetArray, intHashBin.maxBinSize() + 1, cotReceiverOutput);
            cotReceiverOutput = null;
        }
        stopWatch.stop();
        long bspTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, bspTime);

        stopWatch.start();
        MspCotReceiverOutput receiverOutput = generateReceiverOutput(bspCotReceiverOutput);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private List<byte[]> generateHashKeysPayload() {
        // generate sparse points
        TIntSet itemSet = new TIntHashSet(t);
        while (itemSet.size() < t) {
            itemSet.add(secureRandom.nextInt(num));
        }
        alphaArray = itemSet.toArray();
        intNoStashCuckooHashBin = IntCuckooHashBinFactory.createEnforceInstance(
            envType, Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE, alphaArray.length, alphaArray, secureRandom
        );
        return Arrays.stream(intNoStashCuckooHashBin.getHashKeys()).collect(Collectors.toList());
    }

    private int[] generateTargetArray() {
        // R independently builds m buckets {B_j}_{j ∈ [m]} with B_j = {x ∈ [n] | ∃i ∈ [τ]: h_i(x) = j}
        int m = intNoStashCuckooHashBin.binNum();
        // Initialize m empty buckets {B_j}_{j ∈ [m]}.
        intHashBin = new SimpleIntHashBin(envType, m, num, intNoStashCuckooHashBin.getHashKeys());
        // For each x ∈ [n], i ∈ [τ], compute j := h_i(x) and add x into bucket B_j.
        int[] xs = IntStream.range(0, num).toArray();
        intHashBin.insertItems(xs);
        positionMaps = IntStream.range(0, m)
            .mapToObj(binIndex -> {
                // Sort all values in each bucket in an increasing order.
                int[] bin = Arrays.stream(intHashBin.getBin(binIndex))
                    .filter(item -> item >= 0)
                    .distinct()
                    .sorted()
                    .toArray();
                // Define a function pos_j : B_j → [|Bj|] to map a value into its position in the j-th bucket B_j.
                TIntIntMap positionMap = new TIntIntHashMap(bin.length);
                for (int position = 0; position < bin.length; position++) {
                    positionMap.put(bin[position], position);
                }
                return positionMap;
            })
            .collect(Collectors.toCollection(ArrayList::new));
        // For each j ∈ [m]
        return IntStream.range(0, m)
            .map(binIndex -> {
                int tj = intNoStashCuckooHashBin.getBinEntry(binIndex);
                if (tj < 0) {
                    // R sets p_j = |B_j| + 1 if T[j] = ⊥
                    return intHashBin.maxBinSize();
                } else {
                    // R sets p_j = pos_j(T[j]) otherwise.
                    return positionMaps.get(binIndex).get(tj);
                }
            })
            .toArray();
    }

    private MspCotReceiverOutput generateReceiverOutput(BspCotReceiverOutput bspCotReceiverOutput) {
        // For each x ∈ [n]
        IntStream nIntStream = IntStream.range(0, num);
        nIntStream = parallel ? nIntStream.parallel() : nIntStream;
        byte[][] rbArray = nIntStream
            .mapToObj(x -> {
                // R computes r[x] = Σ_{i ∈ [τ]} r_{h_i(x)}[pos_{h_i(x)}(x)]
                int[] binIndexes = Arrays.stream(intHashBin.getItemBinIndexes(x)).distinct().toArray();
                byte[] rx = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                for (int binIndex : binIndexes) {
                    BytesUtils.xori(rx, bspCotReceiverOutput.get(binIndex).getRb(positionMaps.get(binIndex).get(x)));
                }
                return rx;
            })
            .toArray(byte[][]::new);
        intHashBin = null;
        positionMaps = null;
        return MspCotReceiverOutput.create(alphaArray, rbArray);
    }
}
