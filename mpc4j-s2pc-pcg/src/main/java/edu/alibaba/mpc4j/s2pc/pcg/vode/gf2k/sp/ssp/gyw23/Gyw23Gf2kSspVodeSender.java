package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.gyw23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeSender;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.AbstractGf2kSspVodeSender;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.gyw23.Gyw23Gf2kSspVodePtoDesc.PtoStep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * GYW23 GF2K-SSP-VODE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Gyw23Gf2kSspVodeSender extends AbstractGf2kSspVodeSender {
    /**
     * core COT
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * core GF2K-VODE sender
     */
    private final Gf2kCoreVodeSender gf2kCoreVodeSender;
    /**
     * hash that satisfies circular correlation robustness
     */
    private final Crhf hash;
    /**
     * GF2K-VODE sender output
     */
    private Gf2kVodeSenderOutput gf2kVodeSenderOutput;
    /**
     * tree depth
     */
    private int h;
    /**
     * α_1 ... α_h
     */
    private boolean[] binaryAlpha;
    /**
     * !α_1 ... !α_h
     */
    private boolean[] notBinaryAlpha;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * the GGM tree. It contains (l + 1)-level keys. The i-th level contains 2^i keys.
     */
    private ArrayList<byte[][]> ggmTree;

    public Gyw23Gf2kSspVodeSender(Rpc senderRpc, Party receiverParty, Gyw23Gf2kSspVodeConfig config) {
        super(Gyw23Gf2kSspVodePtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        gf2kCoreVodeSender = Gf2kCoreVodeFactory.createSender(senderRpc, receiverParty, config.getGf2kCoreVodeConfig());
        addSubPto(gf2kCoreVodeSender);
        hash = CrhfFactory.createInstance(envType, CrhfType.MMO);
    }

    @Override
    public void init(int subfieldL) throws MpcAbortException {
        setInitInput(subfieldL);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotReceiver.init();
        gf2kCoreVodeSender.init(subfieldL);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kSspVodeSenderOutput send(int alpha, int num) throws MpcAbortException {
        setPtoInput(alpha, num);
        return send();
    }

    @Override
    public Gf2kSspVodeSenderOutput send(int alpha, int num, Gf2kVodeSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(alpha, num, preSenderOutput);
        gf2kVodeSenderOutput = preSenderOutput;
        return send();
    }

    private Gf2kSspVodeSenderOutput send() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        Gf2kSspVodeSenderOutput senderOutput;
        if (num == 1) {
            // we directly use (0, β, M[β]) as output since M[β] = K[β] + β · Γ.
            assert alpha == 0;
            stopWatch.start();
            Gf2kVodeSenderOutput correctGf2kVodeSenderOutput = correctVode();
            stopWatch.stop();
            long vodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 2, vodeTime);

            stopWatch.start();
            assert correctGf2kVodeSenderOutput.getNum() == 1;
            byte[] beta = correctGf2kVodeSenderOutput.getX(0);
            byte[] mBeta = correctGf2kVodeSenderOutput.getT(0);
            senderOutput = Gf2kSspVodeSenderOutput.create(
                field, alpha, beta, new byte[][]{mBeta}
            );
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);
        } else {
            stopWatch.start();
            Gf2kVodeSenderOutput correctGf2kVodeSenderOutput = correctVode();
            stopWatch.stop();
            long vodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 3, vodeTime, "Sender corrects sVODE");

            stopWatch.start();
            h = LongUtils.ceilLog2(num, 1);
            // computes α_1 ... α_h and !α_1 ... !α_h
            int offset = Integer.SIZE - h;
            binaryAlpha = new boolean[h];
            notBinaryAlpha = new boolean[h];
            byte[] alphaBytes = IntUtils.intToByteArray(alpha);
            IntStream.range(0, h).forEach(i -> {
                binaryAlpha[i] = BinaryUtils.getBoolean(alphaBytes, offset + i);
                notBinaryAlpha[i] = !binaryAlpha[i];
            });
            // P0 and P1 send (extend, n) to F_COT, which returns (K[r_1], . . . , K[r_n]) ∈ F^n_{2^λ} to P0 and
            // ((r_1, ..., r_n), (M[r_1], ..., M[r_n])) ∈ F_2^n × F^n_{2λ} to P1
            // such that M[r_i] = K[r_i] ⊕ r_i · ∆ for i ∈ [1, n]. Here we use α = α_1...α_n := !r_1...!r_n
            cotReceiverOutput = coreCotReceiver.receive(notBinaryAlpha);
            stopWatch.stop();
            long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, cotTime, "Sender executes COT");

            List<byte[]> correlationPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_CORRELATION.ordinal());

            stopWatch.start();
            if (num == 2) {
                handleNum2CorrelationPayload(correctGf2kVodeSenderOutput, correlationPayload);
            } else {
                handleCorrelationPayload(correctGf2kVodeSenderOutput, correlationPayload);
            }
            // P1 outputs (u, w), we need to reduce num
            byte[] beta = correctGf2kVodeSenderOutput.getX(0);
            byte[][] ws = ggmTree.get(h);
            if (num < (1 << h)) {
                byte[][] reduceWs = new byte[num][];
                System.arraycopy(ws, 0, reduceWs, 0, num);
                ws = reduceWs;
            }
            senderOutput = Gf2kSspVodeSenderOutput.create(field, alpha, beta, ws);
            ggmTree = null;
            stopWatch.stop();
            long ggmTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, ggmTime, "Sender handles GGT tree");

        }
        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private Gf2kVodeSenderOutput correctVode() throws MpcAbortException {
        // P0 and P1 send (extend, 1) to F_sVODE,
        // which returns K[s] ∈ K to P0 and (s, M[s]) ∈ F × K to P1 such that M[s] = K[s] + s · Γ.
        int preVodeNum = Gf2kSspVodeFactory.getPrecomputeNum(config, subfieldL, num);
        assert preVodeNum == 1;
        if (gf2kVodeSenderOutput == null) {
            byte[][] xs = IntStream.range(0, preVodeNum)
                .mapToObj(index -> subfield.createNonZeroRandom(secureRandom))
                .toArray(byte[][]::new);
            gf2kVodeSenderOutput = gf2kCoreVodeSender.send(xs);
        } else {
            gf2kVodeSenderOutput.reduce(preVodeNum);
        }
        // P1 samples β ← F^∗, sets M[β] := M[s], and sends d := s − β ∈ F to P0
        byte[] s = gf2kVodeSenderOutput.getX(0);
        byte[] mBeta = gf2kVodeSenderOutput.getT(0);
        byte[] beta = subfield.createNonZeroRandom(secureRandom);
        assert subfield.validateNonZeroElement(beta);
        byte[] d = subfield.sub(beta, s);
        List<byte[]> dPayload = Collections.singletonList(d);
        sendOtherPartyPayload(PtoStep.SENDER_SEND_D.ordinal(), dPayload);
        gf2kVodeSenderOutput = null;
        return Gf2kVodeSenderOutput.create(field, new byte[][]{beta}, new byte[][]{mBeta});
    }

    private void handleNum2CorrelationPayload(Gf2kVodeSenderOutput correctGf2kVodeSenderOutput,
                                              List<byte[]> correlationPayload) throws MpcAbortException {
        assert h == 1;
        MpcAbortPreconditions.checkArgument(correlationPayload.size() == h + 3);
        byte[][] corrections = correlationPayload.toArray(new byte[0][]);
        // phase corrections
        byte[] mu = corrections[h - 1];
        byte[] cn0 = corrections[h];
        byte[] cn1 = corrections[h + 1];
        byte[] phi = corrections[h + 2];
        // create ggm three
        ggmTree = new ArrayList<>(h + 1);
        // place the level-0 key with an empty key
        ggmTree.add(new byte[0][]);
        byte[][] lastLevel = new byte[1 << h][];
        byte[] kn = notBinaryAlpha[h - 1] ? cn1 : cn0;
        field.subi(kn, hash.hash(BytesUtils.xor(mu, cotReceiverOutput.getRb(h - 1))));
        int alphaStar = notBinaryAlpha[h - 1] ? 1 : 0;
        lastLevel[alphaStar] = kn;
        lastLevel[alpha] = field.createZero();
        field.addi(lastLevel[alpha], phi);
        field.addi(lastLevel[alpha], correctGf2kVodeSenderOutput.getT(0));
        field.subi(lastLevel[alpha], kn);
        ggmTree.add(lastLevel);
        cotReceiverOutput = null;
        binaryAlpha = null;
        notBinaryAlpha = null;
    }

    private void handleCorrelationPayload(Gf2kVodeSenderOutput correctGf2kVodeSenderOutput,
                                          List<byte[]> correlationPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(correlationPayload.size() == h + 3);
        byte[][] corrections = correlationPayload.toArray(new byte[0][]);
        // phase corrections
        byte[][] cns = new byte[h - 1][];
        System.arraycopy(corrections, 0, cns, 0, h - 1);
        byte[] mu = corrections[h - 1];
        byte[] cn0 = corrections[h];
        byte[] cn1 = corrections[h + 1];
        byte[] phi = corrections[h + 2];
        // set K_i^{!α_i} := M[r_i] ⊕ c_i for i ∈ [1, n - 1]
        byte[][] kbs = new byte[h - 1][];
        System.arraycopy(cns, 0, kbs, 0, h - 1);
        for (int i = 0; i < h - 1; i++) {
            BytesUtils.xori(kbs[i], cotReceiverOutput.getRb(i));
        }
        // K_n^{!α_n} = c_n^{r_n} - H(µ ⊕ M[r_n])
        byte[] kn = notBinaryAlpha[h - 1] ? cn1 : cn0;
        field.subi(kn, hash.hash(BytesUtils.xor(mu, cotReceiverOutput.getRb(h - 1))));
        // create ggm three
        ggmTree = new ArrayList<>(h + 1);
        // place the level-0 key with an empty key
        ggmTree.add(new byte[0][]);
        int alphaPrefix = 0;
        // For each i ∈ {1,...,h}
        for (int i = 1; i <= h - 1; i++) {
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
                        currentLevel[2 * j + 1] = BytesUtils.xor(previousLevel[j], currentLevel[2 * j]);
                    }
                }
                // compute the remaining seeds
                int alphaStar = (alphaPrefix << 1) + notAlphaiInt;
                currentLevel[alphaStar] = kb;
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
        byte[][] previousLastLevel = ggmTree.get(h - 1);
        byte[][] lastLevel = new byte[1 << h][];
        boolean notAlphaH = notBinaryAlpha[h - 1];
        int intAlphaH = notAlphaH ? 0 : 1;
        int intNotAlphaH = notAlphaH ? 1 : 0;
        byte[] one = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        Arrays.fill(one, (byte) 0b11111111);
        // for j ∈ [0, 2^{n−1}), j != α_1 ... α_{n−1}, b ∈ {0, 1} do: X_n^{2j+b} = H(X^j_{n - 1} ⊕ b)
        for (int j = 0; j < (1 << (h - 1)); j++) {
            if (j != alphaPrefix) {
                // K_i^{2j} = H(K_{i - 1}^{j})
                lastLevel[2 * j] = hash.hash(previousLastLevel[j]);
                // K_i^{2j + 1} = K_{i - 1}^{j} - K_i^{2j}
                lastLevel[2 * j + 1] = hash.hash(BytesUtils.xor(previousLastLevel[j], one));
            }
        }
        // X^{α_1 ... α_{n−1} !α_n} = K_n^{!α_n} - Σ_{j ∈ [0, 2^h), j ≠ α} {X_n^{2j + !α_n}}
        int alphaStar = (alphaPrefix << 1) + intNotAlphaH;
        lastLevel[alphaStar] = kn;
        byte[] sum = field.createZero();
        for (int j = 0; j < (1 << (h - 1)); j++) {
            if (j != alphaPrefix) {
                field.addi(sum, lastLevel[2 * j + intNotAlphaH]);
            }
        }
        field.subi(lastLevel[alphaStar], sum);
        // X_n^α = γ − Σ_{j ∈ [0, 2^h), j ≠ α} {X_j}, where γ = ψ + M[β]
        assert alpha == (alphaPrefix << 1) + intAlphaH;
        sum = field.createZero();
        for (int j = 0; j < (1 << (h)); j++) {
            if (j != alpha) {
                field.addi(sum, lastLevel[j]);
            }
        }
        lastLevel[alpha] = field.createZero();
        field.addi(lastLevel[alpha], phi);
        field.addi(lastLevel[alpha], correctGf2kVodeSenderOutput.getT(0));
        field.subi(lastLevel[alpha], sum);
        ggmTree.add(lastLevel);
        cotReceiverOutput = null;
        binaryAlpha = null;
        notBinaryAlpha = null;
    }
}
