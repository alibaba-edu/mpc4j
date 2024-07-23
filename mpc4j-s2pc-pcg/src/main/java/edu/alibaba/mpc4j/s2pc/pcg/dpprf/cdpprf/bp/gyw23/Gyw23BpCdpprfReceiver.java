package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.gyw23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.AbstractBpCdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.gyw23.Gyw23BpCdpprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotReceiver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GYW23-BP-CDPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public class Gyw23BpCdpprfReceiver extends AbstractBpCdpprfReceiver {
    /**
     * core COT
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * pre-compute COT
     */
    private final PreCotReceiver preCotReceiver;
    /**
     * hash that satisfies circular correlation robustness
     */
    private final Crhf hash;
    /**
     * tree depth
     */
    private int h;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * α_1 ... α_h
     */
    private boolean[][] binaryAlphaArray;
    /**
     * !α_1 ... !α_h
     */
    private boolean[][] notBinaryAlphaArray;
    /**
     * the final level of the GGM trees. Each tree contains (l + 1)-level keys. The i-th level contains 2^i keys.
     */
    private ArrayList<byte[][]> ggmResults;

    public Gyw23BpCdpprfReceiver(Rpc receiverRpc, Party senderParty, Gyw23BpCdpprfConfig config) {
        super(Gyw23BpCdpprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        preCotReceiver = PreCotFactory.createReceiver(receiverRpc, senderParty, config.getPreCotConfig());
        addSubPto(preCotReceiver);
        hash = CrhfFactory.createInstance(envType, CrhfType.MMO);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotReceiver.init();
        preCotReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BpCdpprfReceiverOutput puncture(int[] alphaArray, int eachNum) throws MpcAbortException {
        setPtoInput(alphaArray, eachNum);
        return puncture();
    }

    @Override
    public BpCdpprfReceiverOutput puncture(int[] alphaArray, int eachNum, CotReceiverOutput preReceiverOutput)
        throws MpcAbortException {
        setPtoInput(alphaArray, eachNum, preReceiverOutput);
        cotReceiverOutput = preReceiverOutput;
        return puncture();
    }

    private BpCdpprfReceiverOutput puncture() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        h = LongUtils.ceilLog2(eachNum, 1);
        // P_1 send (extend, 1) to F_COT, which returns (r_1, M[r_1] ∈ {0,1} × {0,1}^κ to P_1
        if (cotReceiverOutput == null) {
            boolean[] rs = BinaryUtils.randomBinary(cotNum, secureRandom);
            cotReceiverOutput = coreCotReceiver.receive(rs);
        } else {
            cotReceiverOutput.reduce(cotNum);
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cotTime);

        SpCdpprfReceiverOutput[] receiverOutputs;
        if (eachNum == 1) {
            assert cotNum == batchNum && h == 1;
            stopWatch.start();
            boolean[] choices = new boolean[batchNum];
            Arrays.fill(choices, true);
            cotReceiverOutput = preCotReceiver.receive(cotReceiverOutput, choices);
            stopWatch.stop();
            long updateCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, updateCotTime);

            stopWatch.start();
            receiverOutputs = IntStream.range(0, batchNum)
                .mapToObj(batchIndex -> {
                    assert alphaArray[batchIndex] == 0;
                    return new SpCdpprfReceiverOutput(alphaArray[batchIndex], new byte[1][]);
                })
                .toArray(SpCdpprfReceiverOutput[]::new);
            cotReceiverOutput = null;
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, outputTime);
        } else {
            logPhaseInfo(PtoState.PTO_BEGIN);

            stopWatch.start();
            // computes α_1 ... α_h and !α_1 ... !α_h
            int offset = Integer.SIZE - h;
            binaryAlphaArray = new boolean[batchNum][h];
            notBinaryAlphaArray = new boolean[batchNum][h];
            boolean[] flattenNotBinaryAlphaArray = new boolean[h * batchNum];
            IntStream.range(0, batchNum).forEach(batchIndex -> {
                int alpha = alphaArray[batchIndex];
                byte[] alphaBytes = IntUtils.intToByteArray(alpha);
                IntStream.range(0, h).forEach(i -> {
                    binaryAlphaArray[batchIndex][i] = BinaryUtils.getBoolean(alphaBytes, offset + i);
                    notBinaryAlphaArray[batchIndex][i] = !binaryAlphaArray[batchIndex][i];
                    flattenNotBinaryAlphaArray[batchIndex * h + i] = notBinaryAlphaArray[batchIndex][i];
                });
            });
            cotReceiverOutput = preCotReceiver.receive(cotReceiverOutput, flattenNotBinaryAlphaArray);
            stopWatch.stop();
            long updateCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, updateCotTime);

            List<byte[]> correlationPayload = receiveOtherPartyEqualSizePayload(
                PtoStep.SENDER_SEND_CORRELATION.ordinal(), h * batchNum, CommonConstants.BLOCK_BYTE_LENGTH);

            stopWatch.start();
            handleCorrelationPayload(correlationPayload);
            receiverOutputs = generateReceiverOutput();
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, outputTime);
        }
        logPhaseInfo(PtoState.PTO_END);
        return new BpCdpprfReceiverOutput(receiverOutputs);
    }

    private void handleCorrelationPayload(List<byte[]> correlationPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(correlationPayload.size() == h * batchNum);
        byte[][] kbsArray = correlationPayload.toArray(new byte[0][]);
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        ggmResults = batchIndexIntStream
            .mapToObj(batchIndex -> {
                // set K_i^{!α_i} := M[r_i] ⊕ c_i for i ∈ [1, n]
                for (int i = 0; i < h; i++) {
                    BytesUtils.xori(kbsArray[batchIndex * h + i], cotReceiverOutput.getRb(batchIndex * h + i));
                }
                ArrayList<byte[][]> ggmTree = new ArrayList<>(h + 1);
                // place the level-0 key with an empty key
                ggmTree.add(new byte[0][]);
                int alphaPrefix = 0;
                // For each i ∈ {1,...,h}
                for (int i = 1; i <= h; i++) {
                    int hIndex = i - 1;
                    byte[][] currentLevel = new byte[1 << i][CommonConstants.BLOCK_BYTE_LENGTH];
                    // R defines an i-bit string α_i^* = α_1 ... α_{i − 1} !α_i
                    boolean alphai = binaryAlphaArray[batchIndex][hIndex];
                    int alphaiInt = alphai ? 1 : 0;
                    boolean notAlphai = notBinaryAlphaArray[batchIndex][hIndex];
                    int notAlphaiInt = notAlphai ? 1 : 0;
                    byte[] kb = kbsArray[batchIndex * h + hIndex];
                    if (i == 1) {
                        // If i = 1, define K_{!α_i}^i = K_{!α_i}^i
                        currentLevel[alphaiInt] = null;
                        currentLevel[notAlphaiInt] = kb;
                    } else {
                        // If i ≥ 2
                        byte[][] previousLevel = ggmTree.get(i - 1);
                        // for j ∈ [2^i − 1], j ≠ α_1...α_{i − 1}
                        for (int j = 0; j < (1 << (i - 1)); j++) {
                            if (j != alphaPrefix) {
                                // K_i^{2j} = H(K_{i - 1}^{j})
                                currentLevel[2 * j] = hash.hash(previousLevel[j]);
                                // K_i^{2j + 1} = K_{i - 1}^{j} - K_i^{2j}
                                BytesUtils.xori(currentLevel[2 * j + 1], previousLevel[j]);
                                BytesUtils.xori(currentLevel[2 * j + 1], currentLevel[2 * j]);
                            }
                        }
                        // compute the remaining seeds
                        int alphaStar = (alphaPrefix << 1) + notAlphaiInt;
                        currentLevel[alphaStar] = kb;
                        currentLevel[(alphaPrefix << 1) + alphaiInt] = null;
                        for (int j = 0; j < (1 << (i - 1)); j++) {
                            if (j != alphaPrefix) {
                                BytesUtils.xori(currentLevel[alphaStar], currentLevel[2 * j + notAlphaiInt]);
                            }
                        }
                    }
                    // update α_1...α_{i − 1}
                    alphaPrefix = (alphaPrefix << 1) + alphaiInt;
                    ggmTree.add(currentLevel);
                }
                return ggmTree.get(h);
            })
            .collect(Collectors.toCollection(ArrayList::new));
        cotReceiverOutput = null;
        binaryAlphaArray = null;
        notBinaryAlphaArray = null;
    }

    private SpCdpprfReceiverOutput[] generateReceiverOutput() {
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        return batchIndexIntStream
            .mapToObj(batchIndex -> {
                int alpha = alphaArray[batchIndex];
                // R sets w[i] = X_i^h for i ∈ [n] \ {α}
                byte[][] rbArray = ggmResults.get(batchIndex);
                return new SpCdpprfReceiverOutput(alpha, rbArray);
            })
            .toArray(SpCdpprfReceiverOutput[]::new);
    }
}
