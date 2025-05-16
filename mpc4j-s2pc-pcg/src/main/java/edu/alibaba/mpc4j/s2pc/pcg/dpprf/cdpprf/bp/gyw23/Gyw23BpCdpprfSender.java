package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.gyw23;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.AbstractBpCdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.gyw23.Gyw23BpCdpprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GYW23-BP-CDPPRF sender.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public class Gyw23BpCdpprfSender extends AbstractBpCdpprfSender {
    /**
     * core COT
     */
    private final CoreCotSender coreCotSender;
    /**
     * pre-compute COT
     */
    private final PreCotSender preCotSender;
    /**
     * hash that satisfies circular correlation robustness
     */
    private final Crhf hash;
    /**
     * tree depth
     */
    private int h;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * the final level of the GGM trees. Each tree contains (l + 1)-level keys. The i-th level contains 2^i keys.
     */
    private ArrayList<byte[][]> ggmResults;

    /**
     * K_0^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j}^i
     */
    private byte[][][] k0sArray;

    public Gyw23BpCdpprfSender(Rpc senderRpc, Party receiverParty, Gyw23BpCdpprfConfig config) {
        super(Gyw23BpCdpprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        preCotSender = PreCotFactory.createSender(senderRpc, receiverParty, config.getPreCotConfig());
        addSubPto(preCotSender);
        hash = CrhfFactory.createInstance(envType, CrhfType.MMO);
    }

    @Override
    public void init(byte[] delta) throws MpcAbortException {
        setInitInput(delta);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotSender.init(delta);
        preCotSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BpCdpprfSenderOutput puncture(int batchNum, int eachNum) throws MpcAbortException {
        setPtoInput(batchNum, eachNum);
        return puncture();
    }

    @Override
    public BpCdpprfSenderOutput puncture(int batchNum, int eachNum, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(batchNum, eachNum, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return puncture();
    }

    private BpCdpprfSenderOutput puncture() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        byte[] actualDelta;
        h = LongUtils.ceilLog2(eachNum, 1);
        // P_0 send (extend, h) to F_COT, which returns (K[r_1], ..., K[r_n]) ∈ {{0,1}^κ}^h to P_0
        if (cotSenderOutput == null) {
            cotSenderOutput = coreCotSender.send(cotNum);
            actualDelta = delta;
        } else {
            cotSenderOutput.reduce(cotNum);
            actualDelta = cotSenderOutput.getDelta();
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cotTime);

        SpCdpprfSenderOutput[] senderOutputs;
        if (eachNum == 1) {
            assert cotNum == batchNum;
            stopWatch.start();
            cotSenderOutput = preCotSender.send(cotSenderOutput);
            stopWatch.stop();
            long updateCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, updateCotTime);

            stopWatch.start();
            senderOutputs = IntStream.range(0, batchNum)
                .mapToObj(batchIndex -> new SpCdpprfSenderOutput(actualDelta, new byte[][]{actualDelta}))
                .toArray(SpCdpprfSenderOutput[]::new);
            cotSenderOutput = null;
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, outputTime);
        } else {
            stopWatch.start();
            cotSenderOutput = preCotSender.send(cotSenderOutput);
            stopWatch.stop();
            long updateCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, updateCotTime);

            stopWatch.start();
            // P_0 computes the GGM tree
            generateGgmTree(actualDelta);
            List<byte[]> correlationPayload = generateCorrelationPayload();
            sendOtherPartyEqualSizePayload(PtoStep.SENDER_SEND_CORRELATION.ordinal(), correlationPayload);
            senderOutputs = generateSenderOutput(actualDelta);
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, outputTime);
        }
        logPhaseInfo(PtoState.PTO_END);
        return new BpCdpprfSenderOutput(senderOutputs);
    }

    private void generateGgmTree(byte[] actualDelta) {
        k0sArray = IntStream.range(0, batchNum)
            .mapToObj(i -> BlockUtils.zeroBlocks(h))
            .toArray(byte[][][]::new);
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        // find secureRandom.nextBytes(level1[0]); will output the same randomness, change to prg
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        ggmResults = batchIndexIntStream
            .mapToObj(batchIndex -> {
                ArrayList<byte[][]> ggmTree = new ArrayList<>(h + 1);
                // treat Δ as the root node
                ggmTree.add(new byte[][]{actualDelta});
                // X_1^0 = k
                byte[][] level1 = BlockUtils.zeroBlocks(2);
                secureRandom.nextBytes(level1[0]);
                // X_1^1 = Δ - k
                BlockUtils.xori(level1[1], actualDelta);
                BlockUtils.xori(level1[1], level1[0]);
                // K_1^0 = X_1^0
                BlockUtils.xori(k0sArray[batchIndex][0], level1[0]);
                // the first level should use randomness
                ggmTree.add(level1);
                // For i ∈ {1,...,h}, j ∈ [2^{i − 1}], do X_i^{2j} = H(X_{i - 1}^j), X_i^{2j + 1} = X_{i - 1}^j - X_i^{2j}
                for (int i = 2; i <= h; i++) {
                    byte[][] previousLowLevel = ggmTree.get(i - 1);
                    byte[][] currentLevel = BlockUtils.zeroBlocks(1 << i);
                    for (int j = 0; j < (1 << (i - 1)); j++) {
                        // X_i^{2j} = H(X_{i - 1}^j)
                        currentLevel[2 * j] = hash.hash(previousLowLevel[j]);
                        BlockUtils.xori(currentLevel[2 * j + 1], previousLowLevel[j]);
                        BlockUtils.xori(currentLevel[2 * j + 1], currentLevel[2 * j]);
                        // K_i^0 = ⊕_{j ∈ [2^{i - 1}]} X_i^{2j}
                        BlockUtils.xori(k0sArray[batchIndex][i - 1], currentLevel[2 * j]);
                    }
                    ggmTree.add(currentLevel);
                }
                return ggmTree.get(h);
            })
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<byte[]> generateCorrelationPayload() {
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        List<byte[]> correlationPayload = batchIndexIntStream
            .mapToObj(batchIndex ->
                IntStream.range(0, h)
                    .mapToObj(hIndex -> {
                        // S sends C_i = K_0^i ⊕ K[r_i]
                        byte[] ci = cotSenderOutput.getR0(batchIndex * h + hIndex);
                        BlockUtils.xori(ci, k0sArray[batchIndex][hIndex]);
                        return ci;
                    })
                    .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        k0sArray = null;
        cotSenderOutput = null;
        return correlationPayload;
    }

    private SpCdpprfSenderOutput[] generateSenderOutput(byte[] actualDelta) {
        return IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> new SpCdpprfSenderOutput(actualDelta, ggmResults.get(batchIndex)))
            .toArray(SpCdpprfSenderOutput[]::new);
    }
}
