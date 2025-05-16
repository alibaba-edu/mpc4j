package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.gyw23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.AbstractGf2kBspVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.gyw23.Gyw23Gf2kBspVolePtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleSenderOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GYW23 GF2K-BSP-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/9
 */
public class Gyw23Gf2kBspVoleSender extends AbstractGf2kBspVoleSender {
    /**
     * core COT
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * core GF2K-VOLE sender
     */
    private final Gf2kCoreVoleSender gf2kCoreVoleSender;
    /**
     * hash that satisfies circular correlation robustness
     */
    private final Crhf hash;
    /**
     * GF2K-VOLE sender output
     */
    private Gf2kVoleSenderOutput gf2kVoleSenderOutput;
    /**
     * tree depth
     */
    private int h;
    /**
     * α_1 ... α_h
     */
    private boolean[][] binaryAlphaArray;
    /**
     * !α_1 ... !α_h
     */
    private boolean[][] notBinaryAlphaArray;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * the GGM tree. It contains (l + 1)-level keys. The i-th level contains 2^i keys.
     */
    private ArrayList<ArrayList<byte[][]>> ggmTrees;

    public Gyw23Gf2kBspVoleSender(Rpc senderRpc, Party receiverParty, Gyw23Gf2kBspVoleConfig config) {
        super(Gyw23Gf2kBspVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        gf2kCoreVoleSender = Gf2kCoreVoleFactory.createSender(senderRpc, receiverParty, config.getGf2kCoreVoleConfig());
        addSubPto(gf2kCoreVoleSender);
        hash = CrhfFactory.createInstance(envType, CrhfType.MMO);
    }

    @Override
    public void init(int subfieldL) throws MpcAbortException {
        setInitInput(subfieldL);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotReceiver.init();
        gf2kCoreVoleSender.init(subfieldL);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kBspVoleSenderOutput send(int[] alphaArray, int eachNum) throws MpcAbortException {
        setPtoInput(alphaArray, eachNum);
        return send();
    }

    @Override
    public Gf2kBspVoleSenderOutput send(int[] alphaArray, int eachNum, Gf2kVoleSenderOutput preSenderOutput)
        throws MpcAbortException {
        setPtoInput(alphaArray, eachNum, preSenderOutput);
        gf2kVoleSenderOutput = preSenderOutput;
        return send();
    }

    private Gf2kBspVoleSenderOutput send() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        Gf2kSspVoleSenderOutput[] senderOutputs;
        if (eachNum == 1) {
            // we directly use (0, β, M[β]) as output since M[β] = K[β] + β · Γ.
            for (int batchIndex = 0; batchIndex < batchNum; batchIndex++) {
                assert alphaArray[batchIndex] == 0;
            }
            stopWatch.start();
            Gf2kVoleSenderOutput correctGf2kVoleSenderOutput = correctVole();
            stopWatch.stop();
            long voleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 2, voleTime);

            stopWatch.start();
            assert correctGf2kVoleSenderOutput.getNum() == batchNum;
            senderOutputs = IntStream.range(0, batchNum)
                .mapToObj(batchIndex -> {
                    byte[] beta = correctGf2kVoleSenderOutput.getX(batchIndex);
                    byte[] mBeta = correctGf2kVoleSenderOutput.getT(batchIndex);
                    return Gf2kSspVoleSenderOutput.create(field, alphaArray[batchIndex], beta, new byte[][]{mBeta});
                })
                .toArray(Gf2kSspVoleSenderOutput[]::new);
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);
        } else {
            stopWatch.start();
            Gf2kVoleSenderOutput correctGf2kVoleSenderOutput = correctVole();
            stopWatch.stop();
            long voleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 3, voleTime, "Sender corrects sVOLE");

            stopWatch.start();
            h = LongUtils.ceilLog2(eachNum, 1);
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
            // P0 and P1 send (extend, n) to F_COT, which returns (K[r_1], . . . , K[r_n]) ∈ F^n_{2^λ} to P0 and
            // ((r_1, ..., r_n), (M[r_1], ..., M[r_n])) ∈ F_2^n × F^n_{2λ} to P1
            // such that M[r_i] = K[r_i] ⊕ r_i · ∆ for i ∈ [1, n]. Here we use α = α_1...α_n := !r_1...!r_n
            cotReceiverOutput = coreCotReceiver.receive(flattenNotBinaryAlphaArray);
            stopWatch.stop();
            long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, cotTime, "Sender executes COT");

            DataPacketHeader correlationDataPacketHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CORRELATIONS.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> correlationPayload = rpc.receive(correlationDataPacketHeader).getPayload();

            stopWatch.start();
            if (eachNum == 2) {
                handleNum2CorrelationPayload(correctGf2kVoleSenderOutput, correlationPayload);
            } else {
                handleCorrelationPayload(correctGf2kVoleSenderOutput, correlationPayload);
            }
            // P1 outputs (u, w), we need to reduce num
            senderOutputs = IntStream.range(0, batchNum)
                .mapToObj(batchIndex -> {
                    byte[] beta = correctGf2kVoleSenderOutput.getX(batchIndex);
                    byte[][] ws = ggmTrees.get(batchIndex).get(h);
                    if (eachNum < (1 << h)) {
                        byte[][] reduceWs = new byte[eachNum][];
                        System.arraycopy(ws, 0, reduceWs, 0, eachNum);
                        ws = reduceWs;
                    }
                    return Gf2kSspVoleSenderOutput.create(field, alphaArray[batchIndex], beta, ws);
                })
                .toArray(Gf2kSspVoleSenderOutput[]::new);
            ggmTrees = null;
            stopWatch.stop();
            long ggmTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, ggmTime, "Sender handles GGT tree");

        }
        logPhaseInfo(PtoState.PTO_END);
        return new Gf2kBspVoleSenderOutput(senderOutputs);
    }

    private Gf2kVoleSenderOutput correctVole() throws MpcAbortException {
        // P0 and P1 send (extend, 1) to F_sVOLE,
        // which returns K[s] ∈ K to P0 and (s, M[s]) ∈ F × K to P1 such that M[s] = K[s] + s · Γ.
        int preVoleNum = Gf2kBspVoleFactory.getPrecomputeNum(config, subfieldL, batchNum, eachNum);
        assert preVoleNum == batchNum;
        if (gf2kVoleSenderOutput == null) {
            byte[][] xs = IntStream.range(0, preVoleNum)
                .mapToObj(index -> subfield.createNonZeroRandom(secureRandom))
                .toArray(byte[][]::new);
            gf2kVoleSenderOutput = gf2kCoreVoleSender.send(xs);
        } else {
            gf2kVoleSenderOutput.reduce(preVoleNum);
        }
        // P1 samples β ← F^∗, sets M[β] := M[s], and sends d := s − β ∈ F to P0
        byte[][] mBetas = new byte[batchNum][];
        byte[][] betas = new byte[batchNum][];
        List<byte[]> dsPayload = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                byte[] s = gf2kVoleSenderOutput.getX(batchIndex);
                mBetas[batchIndex] = gf2kVoleSenderOutput.getT(batchIndex);
                betas[batchIndex] = subfield.createNonZeroRandom(secureRandom);
                assert subfield.validateNonZeroElement(betas[batchIndex]);
                return subfield.sub(betas[batchIndex], s);
            })
            .collect(Collectors.toList());
        DataPacketHeader dsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_DS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(dsHeader, dsPayload));
        gf2kVoleSenderOutput = null;
        return Gf2kVoleSenderOutput.create(field, betas, mBetas);
    }

    private void handleNum2CorrelationPayload(Gf2kVoleSenderOutput correctGf2kVoleSenderOutput,
                                              List<byte[]> correlationPayload) throws MpcAbortException {
        assert h == 1;
        MpcAbortPreconditions.checkArgument(correlationPayload.size() == batchNum * (h + 3));
        byte[][] corrections = correlationPayload.toArray(new byte[0][]);
        IntStream batchIntStream = IntStream.range(0, batchNum);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        ggmTrees = batchIntStream
            .mapToObj(batchIndex -> {
                // phase corrections
                int offset = batchIndex * (h + 3);
                int cotOffset = batchIndex * h;
                byte[] mu = corrections[offset + h - 1];
                byte[] cn0 = corrections[offset + h];
                byte[] cn1 = corrections[offset + h + 1];
                byte[] phi = corrections[offset + h + 2];
                // create ggm three
                ArrayList<byte[][]> ggmTree = new ArrayList<>(h + 1);
                // place the level-0 key with an empty key
                ggmTree.add(new byte[0][]);
                byte[][] lastLevel = new byte[1 << h][];
                byte[] kn = notBinaryAlphaArray[batchIndex][h - 1] ? cn1 : cn0;
                field.subi(kn, hash.hash(BlockUtils.xor(mu, cotReceiverOutput.getRb(cotOffset + h - 1))));
                int alphaStar = notBinaryAlphaArray[batchIndex][h - 1] ? 1 : 0;
                lastLevel[alphaStar] = kn;
                lastLevel[alphaArray[batchIndex]] = field.createZero();
                field.addi(lastLevel[alphaArray[batchIndex]], phi);
                field.addi(lastLevel[alphaArray[batchIndex]], correctGf2kVoleSenderOutput.getT(batchIndex));
                field.subi(lastLevel[alphaArray[batchIndex]], kn);
                ggmTree.add(lastLevel);

                return ggmTree;
            })
            .collect(Collectors.toCollection(ArrayList::new));
        cotReceiverOutput = null;
        binaryAlphaArray = null;
        notBinaryAlphaArray = null;
    }

    private void handleCorrelationPayload(Gf2kVoleSenderOutput correctGf2kVoleSenderOutput,
                                          List<byte[]> correlationPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(correlationPayload.size() == batchNum * (h + 3));
        byte[][] corrections = correlationPayload.toArray(new byte[0][]);
        IntStream batchIntStream = IntStream.range(0, batchNum);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        ggmTrees = batchIntStream
            .mapToObj(batchIndex -> {
                // phase corrections
                int offset = batchIndex * (h + 3);
                int cotOffset = batchIndex * h;
                byte[][] cns = new byte[h - 1][];
                System.arraycopy(corrections, offset, cns, 0, h - 1);
                byte[] mu = corrections[offset + h - 1];
                byte[] cn0 = corrections[offset + h];
                byte[] cn1 = corrections[offset + h + 1];
                byte[] phi = corrections[offset + h + 2];
                // set K_i^{!α_i} := M[r_i] ⊕ c_i for i ∈ [1, n - 1]
                byte[][] kbs = new byte[h - 1][];
                System.arraycopy(cns, 0, kbs, 0, h - 1);
                for (int i = 0; i < h - 1; i++) {
                    BlockUtils.xori(kbs[i], cotReceiverOutput.getRb(cotOffset + i));
                }
                // K_n^{!α_n} = c_n^{r_n} - H(µ ⊕ M[r_n])
                byte[] kn = notBinaryAlphaArray[batchIndex][h - 1] ? cn1 : cn0;
                field.subi(kn, hash.hash(BlockUtils.xor(mu, cotReceiverOutput.getRb(cotOffset + h - 1))));
                // create ggm three
                ArrayList<byte[][]> ggmTree = new ArrayList<>(h + 1);
                // place the level-0 key with an empty key
                ggmTree.add(new byte[0][]);
                int alphaPrefix = 0;
                // For each i ∈ {1,...,h}
                for (int i = 1; i <= h - 1; i++) {
                    int hIndex = i - 1;
                    byte[][] currentLevel = new byte[1 << i][];
                    // R defines an i-bit string α_i^* = α_1 ... α_{i − 1} !α_i
                    boolean alphai = binaryAlphaArray[batchIndex][hIndex];
                    int alphaiInt = alphai ? 1 : 0;
                    boolean notAlphai = notBinaryAlphaArray[batchIndex][hIndex];
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
                byte[][] previousLastLevel = ggmTree.get(h - 1);
                byte[][] lastLevel = new byte[1 << h][];
                boolean notAlphaH = notBinaryAlphaArray[batchIndex][h - 1];
                int intAlphaH = notAlphaH ? 0 : 1;
                int intNotAlphaH = notAlphaH ? 1 : 0;
                byte[] one = BlockUtils.allOneBlock();
                // for j ∈ [0, 2^{n−1}), j != α_1 ... α_{n−1}, b ∈ {0, 1} do: X_n^{2j+b} = H(X^j_{n - 1} ⊕ b)
                for (int j = 0; j < (1 << (h - 1)); j++) {
                    if (j != alphaPrefix) {
                        // K_i^{2j} = H(K_{i - 1}^{j})
                        lastLevel[2 * j] = hash.hash(previousLastLevel[j]);
                        // K_i^{2j + 1} = K_{i - 1}^{j} - K_i^{2j}
                        lastLevel[2 * j + 1] = hash.hash(BlockUtils.xor(previousLastLevel[j], one));
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
                assert alphaArray[batchIndex] == (alphaPrefix << 1) + intAlphaH;
                sum = field.createZero();
                for (int j = 0; j < (1 << (h)); j++) {
                    if (j != alphaArray[batchIndex]) {
                        field.addi(sum, lastLevel[j]);
                    }
                }
                lastLevel[alphaArray[batchIndex]] = field.createZero();
                field.addi(lastLevel[alphaArray[batchIndex]], phi);
                field.addi(lastLevel[alphaArray[batchIndex]], correctGf2kVoleSenderOutput.getT(batchIndex));
                field.subi(lastLevel[alphaArray[batchIndex]], sum);
                ggmTree.add(lastLevel);
                return ggmTree;
            })
            .collect(Collectors.toCollection(ArrayList::new));
        cotReceiverOutput = null;
        binaryAlphaArray = null;
        notBinaryAlphaArray = null;
    }
}
