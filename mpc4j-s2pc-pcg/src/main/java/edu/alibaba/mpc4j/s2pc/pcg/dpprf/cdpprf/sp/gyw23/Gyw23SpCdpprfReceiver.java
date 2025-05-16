package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.gyw23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.AbstractSpCdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.gyw23.Gyw23SpCdpprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotReceiver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * GWY23-SP-CDPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public class Gyw23SpCdpprfReceiver extends AbstractSpCdpprfReceiver {
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
    private boolean[] binaryAlpha;
    /**
     * !α_1 ... !α_h
     */
    private boolean[] notBinaryAlpha;
    /**
     * the GGM tree. It contains (l + 1)-level keys. The i-th level contains 2^i keys.
     */
    private ArrayList<byte[][]> ggmTree;

    public Gyw23SpCdpprfReceiver(Rpc receiverRpc, Party senderParty, Gyw23SpCdpprfConfig config) {
        super(Gyw23SpCdpprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
    public SpCdpprfReceiverOutput puncture(int alpha, int num) throws MpcAbortException {
        setPtoInput(alpha, num);
        return puncture();
    }

    @Override
    public SpCdpprfReceiverOutput puncture(int alpha, int num, CotReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(alpha, num, preReceiverOutput);
        cotReceiverOutput = preReceiverOutput;
        return puncture();
    }

    private SpCdpprfReceiverOutput puncture() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        h = LongUtils.ceilLog2(num, 1);
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

        SpCdpprfReceiverOutput receiverOutput;
        if (num == 1) {
            assert alpha == 0 && cotNum == 1 && h == 1;
            stopWatch.start();
            boolean[] choices = new boolean[1];
            Arrays.fill(choices, true);
            cotReceiverOutput = preCotReceiver.receive(cotReceiverOutput, choices);
            stopWatch.stop();
            long updateCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, updateCotTime);

            stopWatch.start();
            receiverOutput = new SpCdpprfReceiverOutput(alpha, new byte[1][]);
            cotReceiverOutput = null;
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, outputTime);
        } else {
            stopWatch.start();
            // computes α_1 ... α_h and !α_1 ... !α_h
            int offset = Integer.SIZE - h;
            binaryAlpha = new boolean[h];
            notBinaryAlpha = new boolean[h];
            byte[] alphaBytes = IntUtils.intToByteArray(alpha);
            IntStream.range(0, h).forEach(i -> {
                binaryAlpha[i] = BinaryUtils.getBoolean(alphaBytes, offset + i);
                notBinaryAlpha[i] = !binaryAlpha[i];
            });
            cotReceiverOutput = preCotReceiver.receive(cotReceiverOutput, notBinaryAlpha);
            stopWatch.stop();
            long updateCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, updateCotTime);

            DataPacketHeader correlationDataPacketHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CORRELATION.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> correlationPayload = rpc.receive(correlationDataPacketHeader).getPayload();

            stopWatch.start();
            handleCorrelationPayload(correlationPayload);
            receiverOutput = generateReceiverOutput();
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, outputTime);
        }
        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private void handleCorrelationPayload(List<byte[]> correlationPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(correlationPayload.size() == h);
        // set K_i^{!α_i} := M[r_i] ⊕ c_i for i ∈ [1, n]
        byte[][] kbs = correlationPayload.toArray(new byte[0][]);
        for (int i = 0; i < h; i++) {
            BlockUtils.xori(kbs[i], cotReceiverOutput.getRb(i));
        }
        ggmTree = new ArrayList<>(h + 1);
        // place the level-0 key with an empty key
        ggmTree.add(new byte[0][]);
        int alphaPrefix = 0;
        // For each i ∈ {1,...,h}
        for (int i = 1; i <= h; i++) {
            int hIndex = i - 1;
            byte[][] currentLevel = new byte[1 << i][];
            // R defines an i-bit string α_i^* = α_1 ... α_{i − 1} !α_i
            boolean alphai = binaryAlpha[hIndex];
            int alphaiInt = alphai ? 1 : 0;
            boolean notAlphai = notBinaryAlpha[hIndex];
            int notAlphaiInt = notAlphai ? 1 : 0;
            byte[] kb = kbs[hIndex];
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
                        currentLevel[2 * j + 1] = BlockUtils.xor(previousLevel[j], currentLevel[2 * j]);
                    }
                }
                // compute the remaining seeds
                int alphaStar = (alphaPrefix << 1) + notAlphaiInt;
                currentLevel[alphaStar] = BlockUtils.zeroBlock();
                BlockUtils.xori(currentLevel[alphaStar], kb);
                for (int j = 0; j < (1 << (i - 1)); j++) {
                    if (j != alphaPrefix) {
                        BlockUtils.xori(currentLevel[alphaStar], currentLevel[2 * j + notAlphaiInt]);
                    }
                }
            }
            // update α_1...α_{i − 1}
            alphaPrefix = (alphaPrefix << 1) + alphaiInt;
            ggmTree.add(currentLevel);
        }
        cotReceiverOutput = null;
        binaryAlpha = null;
        notBinaryAlpha = null;
    }

    private SpCdpprfReceiverOutput generateReceiverOutput() {
        // R sets w[i] = X_i^h for i ∈ [n] \ {α}
        byte[][] rbArray = ggmTree.get(h);
        ggmTree = null;

        return new SpCdpprfReceiverOutput(alpha, rbArray);
    }
}
