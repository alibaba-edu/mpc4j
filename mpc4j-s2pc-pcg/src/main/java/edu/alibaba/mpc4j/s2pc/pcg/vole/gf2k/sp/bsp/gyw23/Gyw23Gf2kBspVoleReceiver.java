package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.gyw23;

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
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.AbstractGf2kBspVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.gyw23.Gyw23Gf2kBspVolePtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleReceiverOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GYW23 GF2K-BSP-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/9
 */
public class Gyw23Gf2kBspVoleReceiver extends AbstractGf2kBspVoleReceiver {
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
    private ArrayList<ArrayList<byte[][]>> ggmTrees;
    /**
     * K_i^0, i ∈ [1, n - 1]
     */
    private byte[][][] knsArray;
    /**
     * K_n^0
     */
    private byte[][] kn0Array;
    /**
     * k_n^1
     */
    private byte[][] kn1Array;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;

    public Gyw23Gf2kBspVoleReceiver(Rpc receiverRpc, Party senderParty, Gyw23Gf2kBspVoleConfig config) {
        super(Gyw23Gf2kBspVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
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
    public Gf2kBspVoleReceiverOutput receive(int batchNum, int eachNum) throws MpcAbortException {
        setPtoInput(batchNum, eachNum);
        return receive();
    }

    @Override
    public Gf2kBspVoleReceiverOutput receive(int batchNum, int eachNum, Gf2kVoleReceiverOutput preReceiverOutput)
        throws MpcAbortException {
        setPtoInput(batchNum, eachNum, preReceiverOutput);
        gf2kVoleReceiverOutput = preReceiverOutput;
        return receive();
    }

    private Gf2kBspVoleReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        Gf2kSspVoleReceiverOutput[] receiverOutputs;
        if (eachNum == 1) {
            // we directly use (0, β, M[β]) as output since M[β] = K[β] + β · Γ.
            stopWatch.start();
            Gf2kVoleReceiverOutput correctGf2kVoleReceiverOutput = correctVole();
            stopWatch.stop();
            long voleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 2, voleTime);

            stopWatch.start();
            assert correctGf2kVoleReceiverOutput.getNum() == batchNum;
            receiverOutputs = IntStream.range(0, batchNum)
                .mapToObj(batchIndex -> {
                    byte[] kBeta = correctGf2kVoleReceiverOutput.getQ(batchIndex);
                    return Gf2kSspVoleReceiverOutput.create(field, delta, new byte[][]{kBeta});
                })
                .toArray(Gf2kSspVoleReceiverOutput[]::new);
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
            h = LongUtils.ceilLog2(eachNum, 1);
            // P0 and P1 send (extend, n) to F_COT, which returns (K[r_1], . . . , K[r_n]) ∈ F^n_{2^λ} to P0 and
            // ((r_1, ..., r_n), (M[r_1], ..., M[r_n])) ∈ F_2^n × F^n_{2λ} to P1
            // such that M[r_i] = K[r_i] ⊕ r_i · ∆ for i ∈ [1, n]. Here we use α = α_1...α_n := !r_1...!r_n
            cotSenderOutput = coreCotSender.send(h * batchNum);
            stopWatch.stop();
            long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, cotTime, "Sender executes COT");

            stopWatch.start();
            List<byte[]> correlationPayload;
            if (eachNum == 2) {
                generateNum2GgmTree();
                correlationPayload = generateNum2CorrelationPayload(correctGf2kVoleReceiverOutput);
            } else {
                generateGgmTree();
                correlationPayload = generateCorrelationPayload(correctGf2kVoleReceiverOutput);
            }
            DataPacketHeader correlationHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CORRELATIONS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(correlationHeader, correlationPayload));
            // P0 outputs v, we need to reduce num
            receiverOutputs = IntStream.range(0, batchNum)
                .mapToObj(batchIndex -> {
                    byte[][] vs = ggmTrees.get(batchIndex).get(h);
                    if (eachNum < (1 << h)) {
                        byte[][] reduceWs = new byte[eachNum][];
                        System.arraycopy(vs, 0, reduceWs, 0, eachNum);
                        vs = reduceWs;
                    }
                    return Gf2kSspVoleReceiverOutput.create(field, delta, vs);
                })
                .toArray(Gf2kSspVoleReceiverOutput[]::new);
            ggmTrees = null;
            stopWatch.stop();
            long ggmTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, ggmTime, "Receiver handles GGT tree");
        }

        logPhaseInfo(PtoState.PTO_END);
        return new Gf2kBspVoleReceiverOutput(receiverOutputs);
    }

    private Gf2kVoleReceiverOutput correctVole() throws MpcAbortException {
        // P0 and P1 send (extend, 1) to F_sVOLE,
        // which returns K[s] ∈ K to P0 and (s, M[s]) ∈ F × K to P1 such that M[s] = K[s] + s · Γ.
        int preVoleNum = Gf2kBspVoleFactory.getPrecomputeNum(config, subfieldL, batchNum, eachNum);
        assert preVoleNum == batchNum;
        if (gf2kVoleReceiverOutput == null) {
            gf2kVoleReceiverOutput = gf2kCoreVoleReceiver.receive(preVoleNum);
        } else {
            gf2kVoleReceiverOutput.reduce(preVoleNum);
        }
        // P1 samples β ← F^∗, sets M[β] := M[s], and sends d := s − β ∈ F to P0
        DataPacketHeader dHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_DS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> dPayload = rpc.receive(dHeader).getPayload();
        MpcAbortPreconditions.checkArgument(dPayload.size() == batchNum);
        byte[][] ds = dPayload.toArray(new byte[0][]);
        // P0 sets K[β] := K[s] + d · Γ such that M[β] = K[β] + β · Γ.
        byte[][] kBetas = new byte[batchNum][];
        for (int batchIndex = 0; batchIndex < batchNum; batchIndex++) {
            kBetas[batchIndex] = gf2kVoleReceiverOutput.getQ(batchIndex);
            field.addi(kBetas[batchIndex], field.mixMul(ds[batchIndex], delta));
        }
        gf2kVoleReceiverOutput = null;
        return Gf2kVoleReceiverOutput.create(field, delta, kBetas);
    }

    private void generateNum2GgmTree() {
        kn0Array = new byte[batchNum][];
        kn1Array = new byte[batchNum][];
        ggmTrees = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                ArrayList<byte[][]> ggmTree = new ArrayList<>(h + 1);
                // treat Δ as the root node
                ggmTree.add(new byte[][]{delta});
                kn0Array[batchIndex] = field.createRandom(secureRandom);
                kn1Array[batchIndex] = field.createRandom(secureRandom);
                ggmTree.add(new byte[][]{kn0Array[batchIndex], kn1Array[batchIndex]});

                return ggmTree;
            })
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private void generateGgmTree() {
        knsArray = new byte[batchNum][h - 1][];
        kn0Array = new byte[batchNum][];
        kn1Array = new byte[batchNum][];
        IntStream batchIntStream = IntStream.range(0, batchNum);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        ggmTrees = batchIntStream
            .mapToObj(batchIndex -> {
                ArrayList<byte[][]> ggmTree = new ArrayList<>(h + 1);
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
                    knsArray[batchIndex][hIndex] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    for (int j = 0; j < (1 << (i - 1)); j++) {
                        BytesUtils.xori(knsArray[batchIndex][hIndex], currentLevel[2 * j]);
                    }
                }
                // K_n^0, K_n^1
                kn0Array[batchIndex] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                kn1Array[batchIndex] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                for (int j = 0; j < (1 << (h - 1)); j++) {
                    field.addi(kn0Array[batchIndex], lastLevel[2 * j]);
                    field.addi(kn1Array[batchIndex], lastLevel[2 * j + 1]);
                }
                return ggmTree;
            })
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<byte[]> generateNum2CorrelationPayload(Gf2kVoleReceiverOutput correctGf2kVoleReceiverOutput) {
        List<byte[]> correlationPayload = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                int cotOffset = batchIndex * h;
                byte[][] collection = new byte[4][];
                byte[] mu = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
                collection[0] = mu;
                // c_n^b := H(µ ⊕ K[r_n] ⊕ b · ∆)) + K_n^b for b ∈ {0, 1}
                byte[] cn0 = BytesUtils.xor(mu, cotSenderOutput.getR0(cotOffset));
                cn0 = hash.hash(cn0);
                field.addi(cn0, kn0Array[batchIndex]);
                collection[1] = cn0;
                byte[] cn1 = BytesUtils.xor(mu, cotSenderOutput.getR1(cotOffset));
                cn1 = hash.hash(cn1);
                field.addi(cn1, kn1Array[batchIndex]);
                collection[2] = cn1;
                // ψ := K_n^0 + K_n^1 - K[β]
                byte[] kBeta = correctGf2kVoleReceiverOutput.getQ(batchIndex);
                byte[] phi = field.add(kn0Array[batchIndex], kn1Array[batchIndex]);
                field.subi(phi, kBeta);
                collection[3] = phi;
                return collection;
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        kn0Array = null;
        kn1Array = null;
        cotSenderOutput = null;
        return correlationPayload;
    }

    private List<byte[]> generateCorrelationPayload(Gf2kVoleReceiverOutput correctGf2kVoleReceiverOutput) {
        IntStream batchIntStream = IntStream.range(0, batchNum);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        List<byte[]> correlationPayload = batchIntStream
            .mapToObj(batchIndex -> {
                int cotOffset = batchIndex * h;
                byte[][] correlations = new byte[h + 3][];
                // c_1 := K[r1] ⊕ k, c_i := K[r_i] ⊕ K_i^0 for i ∈ [2, n − 1]
                for (int i = 0; i < h - 1; i++) {
                    byte[] ci = BytesUtils.xor(knsArray[batchIndex][i], cotSenderOutput.getR0(cotOffset + i));
                    correlations[i] = ci;
                }
                byte[] mu = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
                correlations[h - 1] = mu;
                // c_n^b := H(µ ⊕ K[r_n] ⊕ b · ∆)) + K_n^b for b ∈ {0, 1}
                byte[] cn0 = BytesUtils.xor(cotSenderOutput.getR0(cotOffset + h - 1), mu);
                cn0 = hash.hash(cn0);
                field.addi(cn0, kn0Array[batchIndex]);
                correlations[h] = cn0;
                byte[] cn1 = BytesUtils.xor(cotSenderOutput.getR0(cotOffset + h - 1), mu);
                BytesUtils.xori(cn1, delta);
                cn1 = hash.hash(cn1);
                field.addi(cn1, kn1Array[batchIndex]);
                correlations[h + 1] = cn1;
                // ψ := K_n^0 + K_n^1 - K[β]
                byte[] kBeta = correctGf2kVoleReceiverOutput.getQ(batchIndex);
                byte[] phi = field.add(kn0Array[batchIndex], kn1Array[batchIndex]);
                field.subi(phi, kBeta);
                correlations[h + 2] = phi;
                return correlations;
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        knsArray = null;
        kn0Array = null;
        kn1Array = null;
        cotSenderOutput = null;
        return correlationPayload;
    }
}
