package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.SimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20.Ywl20UniMspCotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.AbstractMspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotSenderOutput;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * YWL20-UNI-MSP-COT sender.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class Ywl20UniMspCotSender extends AbstractMspCotSender {
    /**
     * hash num
     */
    private static final int HASH_NUM = IntCuckooHashBinFactory.getHashNum(Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE);
    /**
     * BSP-COT sender
     */
    private final BspCotSender bspCotSender;
    /**
     * pre-computed COT sender output
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * bin num
     */
    private int m;
    /**
     * hash bin
     */
    private SimpleIntHashBin intHashBin;
    /**
     * position map
     */
    private ArrayList<TIntIntMap> positionMaps;

    public Ywl20UniMspCotSender(Rpc senderRpc, Party receiverParty, Ywl20UniMspCotConfig config) {
        super(Ywl20UniMspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bspCotSender = BspCotFactory.createSender(senderRpc, receiverParty, config.getBspCotConfig());
        addSubPto(bspCotSender);
    }

    @Override
    public void init(byte[] delta, int maxT, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxT, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxBinNum = IntCuckooHashBinFactory.getBinNum(Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE, maxT);
        // In theory, eachNum = maxBinSize. but for larger T, minSize can be small. So we set maxEachNum = HASH_NUM * maxNum
        bspCotSender.init(delta, maxBinNum, HASH_NUM * (maxNum + 1));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MspCotSenderOutput send(int t, int num) throws MpcAbortException {
        setPtoInput(t, num);
        return send();
    }

    @Override
    public MspCotSenderOutput send(int t, int num, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(t, num, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return send();
    }

    private MspCotSenderOutput send() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        DataPacketHeader hashKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> hashKeysPayload = rpc.receive(hashKeysHeader).getPayload();

        stopWatch.start();
        handleHashKeysPayload(hashKeysPayload);
        stopWatch.stop();
        long hashBinTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, hashBinTime);

        stopWatch.start();
        // S sends (sp-extend, |B_j| + 1) to F_{SPCOT}
        BspCotSenderOutput bspCotSenderOutput;
        if (cotSenderOutput == null) {
            bspCotSenderOutput = bspCotSender.send(m, intHashBin.maxBinSize() + 1);
        } else {
            bspCotSenderOutput = bspCotSender.send(m, intHashBin.maxBinSize() + 1, cotSenderOutput);
            cotSenderOutput = null;
        }
        stopWatch.stop();
        long bspTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, bspTime);

        stopWatch.start();
        MspCotSenderOutput senderOutput = generateSenderOutput(bspCotSenderOutput);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private void handleHashKeysPayload(List<byte[]> hashKeysPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(hashKeysPayload.size() == HASH_NUM);
        // S independently builds m buckets {B_j}_{j ∈ [m]} with B_j = {x ∈ [n] | ∃i ∈ [τ]: h_i(x) = j}
        m = IntCuckooHashBinFactory.getBinNum(Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE, t);
        byte[][] keys = hashKeysPayload.toArray(new byte[0][]);
        // Initialize m empty buckets {B_j}_{j ∈ [m]}.
        intHashBin = new SimpleIntHashBin(envType, m, num, keys);
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
    }

    private MspCotSenderOutput generateSenderOutput(BspCotSenderOutput bspCotSenderOutput) {
        // For each x ∈ [n]
        IntStream nIntStream = IntStream.range(0, num);
        nIntStream = parallel ? nIntStream.parallel() : nIntStream;
        byte[][] r0Array = nIntStream
            .mapToObj(x -> {
                // S computes s[x] = Σ_{i ∈ [τ]} s_{h_i(x)}[pos_{h_i(x)}(x)]
                int[] binIndexes = Arrays.stream(intHashBin.getItemBinIndexes(x)).distinct().toArray();
                byte[] sx = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                for (int binIndex : binIndexes) {
                    BytesUtils.xori(sx, bspCotSenderOutput.get(binIndex).getR0(positionMaps.get(binIndex).get(x)));
                }
                return sx;
            })
            .toArray(byte[][]::new);
        intHashBin = null;
        positionMaps = null;
        return MspCotSenderOutput.create(delta, r0Array);
    }
}
