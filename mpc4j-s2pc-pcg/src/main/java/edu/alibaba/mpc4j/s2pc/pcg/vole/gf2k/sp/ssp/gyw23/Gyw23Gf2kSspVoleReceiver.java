package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.gyw23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.AbstractGf2kSspVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.gyw23.Gyw23Gf2kSspVolePtoDesc.PtoStep;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * GYW23-SSP-GF2K-VOLE receive.
 *
 * @author Weiran Liu
 * @date 2024/6/9
 */
public class Gyw23Gf2kSspVoleReceiver extends AbstractGf2kSspVoleReceiver {
    /**
     * core COT
     */
    private final CoreCotSender coreCotSender;
    /**
     * core GF2K-VOLE receiver
     */
    private final Gf2kCoreVoleReceiver gf2kCoreVoleReceiver;
    /**
     * hash that satisfies circular correlation robustness
     */
    private final Crhf hash;
    /**
     * GF2K-VOLE receiver output
     */
    private Gf2kVoleReceiverOutput gf2kVoleReceiverOutput;
    /**
     * tree depth
     */
    private int h;
    /**
     * the GGM tree. It contains (l + 1)-level keys. The i-th level contains 2^i keys.
     */
    private ArrayList<byte[][]> ggmTree;
    /**
     * K_i^0, i ∈ [1, n - 1]
     */
    private byte[][] kns;
    /**
     * K_n^0
     */
    private byte[] kn0;
    /**
     * k_n^1
     */
    private byte[] kn1;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;

    public Gyw23Gf2kSspVoleReceiver(Rpc receiverRpc, Party senderParty, Gyw23Gf2kSspVoleConfig config) {
        super(Gyw23Gf2kSspVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotSender = CoreCotFactory.createSender(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        gf2kCoreVoleReceiver = Gf2kCoreVoleFactory.createReceiver(receiverRpc, senderParty, config.getGf2kCoreVoleConfig());
        addSubPto(gf2kCoreVoleReceiver);
        hash = CrhfFactory.createInstance(envType, CrhfType.MMO);
    }

    @Override
    public void init(int subfieldL, byte[] delta) throws MpcAbortException {
        setInitInput(subfieldL, delta);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotSender.init(delta);
        gf2kCoreVoleReceiver.init(subfieldL, delta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kSspVoleReceiverOutput receive(int num) throws MpcAbortException {
        setPtoInput(num);
        return receive();
    }

    @Override
    public Gf2kSspVoleReceiverOutput receive(int num, Gf2kVoleReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(num, preReceiverOutput);
        gf2kVoleReceiverOutput = preReceiverOutput;
        return receive();
    }

    private Gf2kSspVoleReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        Gf2kSspVoleReceiverOutput receiverOutput;
        if (num == 1) {
            // we directly use (0, β, M[β]) as output since M[β] = K[β] + β · Γ.
            stopWatch.start();
            Gf2kVoleReceiverOutput correctGf2kVoleReceiverOutput = correctVole();
            stopWatch.stop();
            long voleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 2, voleTime);

            stopWatch.start();
            assert correctGf2kVoleReceiverOutput.getNum() == 1;
            byte[] kBeta = correctGf2kVoleReceiverOutput.getQ(0);
            receiverOutput = Gf2kSspVoleReceiverOutput.create(field, delta, new byte[][]{kBeta});
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);
        } else {
            stopWatch.start();
            Gf2kVoleReceiverOutput correctGf2kVoleReceiverOutput = correctVole();
            stopWatch.stop();
            long voleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 3, voleTime, "Sender corrects sVOLE");

            stopWatch.start();
            h = LongUtils.ceilLog2(num, 1);
            // P0 and P1 send (extend, n) to F_COT, which returns (K[r_1], . . . , K[r_n]) ∈ F^n_{2^λ} to P0 and
            // ((r_1, ..., r_n), (M[r_1], ..., M[r_n])) ∈ F_2^n × F^n_{2λ} to P1
            // such that M[r_i] = K[r_i] ⊕ r_i · ∆ for i ∈ [1, n]. Here we use α = α_1...α_n := !r_1...!r_n
            cotSenderOutput = coreCotSender.send(h);
            stopWatch.stop();
            long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, cotTime, "Sender executes COT");

            stopWatch.start();
            List<byte[]> correlationPayload;
            if (num == 2) {
                generateNum2GgmTree();
                correlationPayload = generateNum2CorrelationPayload(correctGf2kVoleReceiverOutput);
            } else {
                generateGgmTree();
                correlationPayload = generateCorrelationPayload(correctGf2kVoleReceiverOutput);
            }
            DataPacketHeader correlationHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CORRELATION.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(correlationHeader, correlationPayload));
            // P0 outputs v, we need to reduce num
            byte[][] vs = ggmTree.get(h);
            if (num < (1 << h)) {
                byte[][] reduceWs = new byte[num][];
                System.arraycopy(vs, 0, reduceWs, 0, num);
                vs = reduceWs;
            }
            receiverOutput = Gf2kSspVoleReceiverOutput.create(field, delta, vs);
            ggmTree = null;
            stopWatch.stop();
            long ggmTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, ggmTime, "Receiver handles GGT tree");
        }

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private Gf2kVoleReceiverOutput correctVole() throws MpcAbortException {
        // P0 and P1 send (extend, 1) to F_sVOLE,
        // which returns K[s] ∈ K to P0 and (s, M[s]) ∈ F × K to P1 such that M[s] = K[s] + s · Γ.
        int preVoleNum = Gf2kSspVoleFactory.getPrecomputeNum(config, subfieldL, num);
        assert preVoleNum == 1;
        if (gf2kVoleReceiverOutput == null) {
            gf2kVoleReceiverOutput = gf2kCoreVoleReceiver.receive(preVoleNum);
        } else {
            gf2kVoleReceiverOutput.reduce(preVoleNum);
        }
        // P1 samples β ← F^∗, sets M[β] := M[s], and sends d := s − β ∈ F to P0
        DataPacketHeader dHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_D.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> dPayload = rpc.receive(dHeader).getPayload();
        MpcAbortPreconditions.checkArgument(dPayload.size() == 1);
        byte[] d = dPayload.get(0);
        // P0 sets K[β] := K[s] + d · Γ such that M[β] = K[β] + β · Γ.
        byte[] kBeta = gf2kVoleReceiverOutput.getQ(0);
        field.addi(kBeta, field.mixMul(d, delta));
        gf2kVoleReceiverOutput = null;
        return Gf2kVoleReceiverOutput.create(field, delta, new byte[][]{kBeta});
    }

    private void generateNum2GgmTree() {
        ggmTree = new ArrayList<>(h + 1);
        // treat Δ as the root node
        ggmTree.add(new byte[][]{delta});
        kn0 = field.createRandom(secureRandom);
        kn1 = field.createRandom(secureRandom);
        ggmTree.add(new byte[][] {kn0, kn1});
    }

    private void generateGgmTree() {
        kns = new byte[h - 1][];
        ggmTree = new ArrayList<>(h + 1);
        // treat Δ as the root node
        ggmTree.add(new byte[][]{delta});
        // X_1^0 = k, later we let c_1 := K[r1] ⊕ k
        byte[][] level1 = new byte[2][CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(level1[0]);
        // X_1^1 = Δ - k
        level1[1] = BytesUtils.xor(delta, level1[0]);
        // the first level should use randomness
        ggmTree.add(level1);
        // For i ∈ {1,...,h - 1}, j ∈ [2^{i − 1}], do X_i^{2j} = H(X_{i - 1}^j), X_i^{2j + 1} = X_{i - 1}^j - X_i^{2j}
        for (int i = 2; i <= h - 1; i++) {
            byte[][] previousLowLevel = ggmTree.get(i - 1);
            byte[][] currentLevel = new byte[1 << i][];
            for (int j = 0; j < (1 << (i - 1)); j++) {
                // X_i^{2j} = H(X_{i - 1}^j)
                currentLevel[2 * j] = hash.hash(previousLowLevel[j]);
                currentLevel[2 * j + 1] = BytesUtils.xor(previousLowLevel[j], currentLevel[2 * j]);
            }
            ggmTree.add(currentLevel);
        }
        // for j ∈ [0, 2^{n − 1}), b ∈ {0, 1} do X_n^{2j+b} := H(X_{n-1}^j ⊕ b)
        byte[][] previousLastLevel = ggmTree.get(h - 1);
        byte[][] lastLevel = new byte[1 << h][];
        byte[] one = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        Arrays.fill(one, (byte) 0b11111111);
        for (int j = 0; j < (1 << (h - 1)); j++) {
            // X_i^{2j} = H(X_{i - 1}^j)
            lastLevel[2 * j] = hash.hash(previousLastLevel[j]);
            lastLevel[2 * j + 1] = hash.hash(BytesUtils.xor(previousLastLevel[j], one));
        }
        ggmTree.add(lastLevel);
        // For each i ∈ {1,...,h - 1}, do K_i^0 = ⊕_{j ∈ [2^{i - 1}]} X_i^{2j}
        for (int i = 1; i <= h - 1; i++) {
            int hIndex = i - 1;
            byte[][] currentLevel = ggmTree.get(i);
            // K_i^0 = ⊕_{j ∈ [2^{i - 1}]} X_i^{2j}
            kns[hIndex] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            for (int j = 0; j < (1 << (i - 1)); j++) {
                BytesUtils.xori(kns[hIndex], currentLevel[2 * j]);
            }
        }
        // K_n^0, K_n^1
        kn0 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        kn1 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        for (int j = 0; j < (1 << (h - 1)); j++) {
            field.addi(kn0, lastLevel[2 * j]);
            field.addi(kn1, lastLevel[2 * j + 1]);
        }
    }

    private List<byte[]> generateNum2CorrelationPayload(Gf2kVoleReceiverOutput correctGf2kVoleReceiverOutput) {
        byte[][] correlation = new byte[4][];
        byte[] mu = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        correlation[0] = mu;
        // c_n^b := H(µ ⊕ K[r_n] ⊕ b · ∆)) + K_n^b for b ∈ {0, 1}
        byte[] cn0 = BytesUtils.xor(mu, cotSenderOutput.getR0(0));
        cn0 = hash.hash(cn0);
        field.addi(cn0, kn0);
        correlation[1] = cn0;
        byte[] cn1 = BytesUtils.xor(mu, cotSenderOutput.getR1(0));
        cn1 = hash.hash(cn1);
        field.addi(cn1, kn1);
        correlation[2] = cn1;
        // ψ := K_n^0 + K_n^1 - K[β]
        byte[] kBeta = correctGf2kVoleReceiverOutput.getQ(0);
        byte[] phi = field.add(kn0, kn1);
        field.subi(phi, kBeta);
        correlation[3] = phi;
        kn0 = null;
        kn1 = null;
        cotSenderOutput = null;
        return Arrays.stream(correlation).collect(Collectors.toList());
    }

    private List<byte[]> generateCorrelationPayload(Gf2kVoleReceiverOutput correctGf2kVoleReceiverOutput) {
        byte[][] correlation = new byte[h + 3][];
        // c_1 := K[r1] ⊕ k, c_i := K[r_i] ⊕ K_i^0 for i ∈ [2, n − 1]
        for (int i = 0; i < h - 1; i++) {
            byte[] ci = BytesUtils.xor(kns[i], cotSenderOutput.getR0(i));
            correlation[i] = ci;
        }
        byte[] mu = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        correlation[h - 1] = mu;
        // c_n^b := H(µ ⊕ K[r_n] ⊕ b · ∆)) + K_n^b for b ∈ {0, 1}
        byte[] cn0 = BytesUtils.xor(cotSenderOutput.getR0(h - 1), mu);
        cn0 = hash.hash(cn0);
        field.addi(cn0, kn0);
        correlation[h] = cn0;
        byte[] cn1 = BytesUtils.xor(cotSenderOutput.getR0(h - 1), mu);
        BytesUtils.xori(cn1, delta);
        cn1 = hash.hash(cn1);
        field.addi(cn1, kn1);
        correlation[h + 1] = cn1;
        // ψ := K_n^0 + K_n^1 - K[β]
        byte[] kBeta = correctGf2kVoleReceiverOutput.getQ(0);
        byte[] phi = field.add(kn0, kn1);
        field.subi(phi, kBeta);
        correlation[h + 2] = phi;
        kns = null;
        kn0 = null;
        kn1 = null;
        cotSenderOutput = null;
        return Arrays.stream(correlation).collect(Collectors.toList());
    }
}