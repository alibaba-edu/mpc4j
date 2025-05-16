package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3Utils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.AbstractF23SowOprfReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F23Wprf;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24.Aprr24F23SowOprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotReceiver;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * APRR24 (F2, F3)-sowOPRF receiver.
 *
 * @author Weiran Liu
 * @date 2024/10/24
 */
public class Aprr24F23SowOprfReceiver extends AbstractF23SowOprfReceiver {
    /**
     * core COT sender, used to generate PRG seeds.
     */
    private final CoreCotSender coreCotSender;
    /**
     * COT receiver, used for OPRF evaluation.
     */
    private final CotReceiver cotReceiver;
    /**
     * pre-computed COT receiver
     */
    private final PreCotReceiver preCotReceiver;
    /**
     * Let G_{i,0} denote stateful PRNGs with F_3 output held by P1 with seeds σ_{i,0}.
     */
    private SecureRandom[] g0Array;
    /**
     * Let G_{i,1} denote stateful PRNGs with F_3 output held by P1 with seeds σ_{i,1}.
     */
    private SecureRandom[] g1Array;

    public Aprr24F23SowOprfReceiver(Rpc receiverRpc, Party senderParty, Aprr24F23SowOprfConfig config) {
        super(Aprr24F23SowOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotSender = CoreCotFactory.createSender(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        addSubPto(cotReceiver);
        preCotReceiver = PreCotFactory.createReceiver(receiverRpc, senderParty, config.getPreCotConfig());
        addSubPto(preCotReceiver);
    }

    @Override
    public void init(int expectBatchSize) throws MpcAbortException {
        setInitInput(expectBatchSize);
        innerInit();
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        innerInit();
    }

    private void innerInit() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = BlockUtils.randomBlock(secureRandom);
        coreCotSender.init(delta);
        // The parties run the setup for Conv32 for m conversions.
        assert expectBatchSize != 0;
        if (expectBatchSize > 0) {
            cotReceiver.init(expectBatchSize * F23Wprf.M);
        } else {
            cotReceiver.init();
        }
        stopWatch.stop();
        long initPtoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, initPtoTime);

        stopWatch.start();
        // The parties perform n random OTs where P0 is OT receiver with choice bit k_i.
        // P1 receives two random strings σ_{i,0}, σ_{i,1} ∈ {0,1}^λ and P0 receives σ_{i,k_i}.
        CotSenderOutput cotSenderOutput = coreCotSender.send(F23Wprf.N);
        RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfType.MMO, cotSenderOutput);
        // Let G′_i denote a stateful PRNG with F_3 output held by P0 with seed σ_{i,k_i}.
        IntStream intStream = parallel ? IntStream.range(0, F23Wprf.N).parallel() : IntStream.range(0, F23Wprf.N);
        g0Array = new SecureRandom[F23Wprf.N];
        g1Array = new SecureRandom[F23Wprf.N];
        intStream.forEach(i -> {
            SecureRandom secureRandom0 = CommonUtils.createSeedSecureRandom();
            secureRandom0.setSeed(rotSenderOutput.getR0(i));
            g0Array[i] = secureRandom0;
            SecureRandom secureRandom1 = CommonUtils.createSeedSecureRandom();
            secureRandom1.setSeed(rotSenderOutput.getR1(i));
            g1Array[i] = secureRandom1;
        });
        preCotReceiver.init();
        stopWatch.stop();
        long initParamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, initParamTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] oprf(byte[][] inputs) throws MpcAbortException {
        setPtoInput(inputs);
        return loopOprf();
    }

    @Override
    public byte[][] oprf(byte[][] inputs, CotReceiverOutput preCotReceiverOutput) throws MpcAbortException {
        setPtoInput(inputs, preCotReceiverOutput);
        return loopOprf();
    }

    private byte[][] loopOprf() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        byte[][] qs;
        if (batchSize > Aprr24F23SowOprfPtoDesc.MAX_BATCH_SIZE) {
            int batchNum = (int) Math.ceil(batchSize * 1.0 / Aprr24F23SowOprfPtoDesc.MAX_BATCH_SIZE);
            qs = new byte[batchSize][];
            for (int i = 0; i < batchNum; i++) {
                int startIndex = i * Aprr24F23SowOprfPtoDesc.MAX_BATCH_SIZE;
                int endIndex = Math.min(startIndex + Aprr24F23SowOprfPtoDesc.MAX_BATCH_SIZE, batchSize);
                byte[][] subInputs = Arrays.copyOfRange(inputs, startIndex, endIndex);
                byte[][] subQs = innerOprf(subInputs, i);
                System.arraycopy(subQs, 0, qs, startIndex, subQs.length);
            }
        } else {
            qs = innerOprf(inputs, 0);
        }

        if (preCotReceiverOutput != null) {
            assert preCotReceiverOutput.getNum() == 0;
            preCotReceiverOutput = null;
        }

        logPhaseInfo(PtoState.PTO_END);
        return qs;
    }

    private byte[][] innerOprf(byte[][] inputs, int currentBatchIndex) throws MpcAbortException {
        stopWatch.start();
        int subBatchSize = inputs.length;
        // P1 computes h_{0,i} ← G_{i,0} for i ∈ [n], h_{1,i} ← G_{i,1} for i ∈ [n]
        byte[][] h0s = new byte[subBatchSize][F23Wprf.N_BYTE_LENGTH];
        byte[][] h1s = new byte[subBatchSize][F23Wprf.N_BYTE_LENGTH];
        IntStream.range(0, subBatchSize).forEach(batchIndex -> {
            for (int i = 0; i < F23Wprf.N; i++) {
                BinaryUtils.setBoolean(h0s[batchIndex], i, g0Array[i].nextBoolean());
                BinaryUtils.setBoolean(h1s[batchIndex], i, g1Array[i].nextBoolean());
            }
        });
        stopWatch.stop();
        long hsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 1, 4, hsTime);

        stopWatch.start();
        // P0 computes f := x ⊕ h0 ⊕ h1
        byte[][] fs = IntStream.range(0, subBatchSize)
            .mapToObj(batchIndex -> {
                byte[] f = BytesUtils.xor(inputs[batchIndex], h0s[batchIndex]);
                BytesUtils.xori(f, h1s[batchIndex]);
                return f;
            })
            .toArray(byte[][]::new);
        List<byte[]> fPayload = Arrays.stream(fs).collect(Collectors.toList());
        sendOtherPartyPayload(PtoStep.RECEIVER_SEND_F.ordinal(), fPayload);
        // P1 computes u := A ·_2 h0, δ := u ⊕ d
        IntStream deltaIntStream = IntStream.range(0, subBatchSize);
        deltaIntStream = parallel ? deltaIntStream.parallel() : deltaIntStream;
        byte[][] ds = deltaIntStream
            .mapToObj(batchIndex -> matrixA.leftMultiply(h0s[batchIndex]))
            .toArray(byte[][]::new);
        stopWatch.stop();
        long fDeltaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 2, 4, fDeltaTime);

        stopWatch.start();
        boolean[] binaryDs = new boolean[subBatchSize * F23Wprf.M];
        for (int batchIndex = 0; batchIndex < subBatchSize; batchIndex++) {
            int offset = batchIndex * F23Wprf.M;
            for (int j = 0; j < F23Wprf.M; j++) {
                binaryDs[offset + j] = BinaryUtils.getBoolean(ds[batchIndex], j);
            }
        }
        CotReceiverOutput cotReceiverOutput;
        if (preCotReceiverOutput == null) {
            cotReceiverOutput = cotReceiver.receive(binaryDs);
        } else {
            cotReceiverOutput = preCotReceiverOutput.split(subBatchSize * F23Wprf.M);
            cotReceiverOutput = preCotReceiver.receive(cotReceiverOutput, binaryDs);
        }
        RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfType.MMO, cotReceiverOutput);
        IntStream sbIntStream = IntStream.range(0, subBatchSize);
        sbIntStream = parallel ? sbIntStream.parallel() : sbIntStream;
        byte[][] sbs = sbIntStream
            .mapToObj(batchIndex -> {
                byte[] sb = new byte[F23Wprf.M];
                int offset = batchIndex * F23Wprf.M;
                for (int j = 0; j < F23Wprf.M; j++) {
                    sb[j] = z3Field.mod(rotReceiverOutput.getRb(offset + j)[0]);
                }
                return sb;
            })
            .toArray(byte[][]::new);
        stopWatch.stop();
        long rotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 3, 4, rotTime);

        List<byte[]> tPayload = receiveOtherPartyPayload(PtoStep.SENDER_SEND_T.ordinal());

        stopWatch.start();
        byte[][] ts = tPayload.stream().toArray(byte[][]::new);
        // P1 computes q := B ·_3 [s′ +_3 (t ⊙ d)].
        IntStream qIntStream = parallel ? IntStream.range(0, subBatchSize).parallel() : IntStream.range(0, subBatchSize);
        byte[][] qs = qIntStream
            .mapToObj(batchIndex -> {
                // (t ⊙ d) -_3 s′ +_3 d
                byte[] t = Z3Utils.decompressFromByteArray(ts[batchIndex], F23Wprf.M);
                int offset = batchIndex * F23Wprf.M;
                byte[] sb = sbs[batchIndex];
                for (int j = 0; j < F23Wprf.M; j++) {
                    boolean dj = binaryDs[offset + j];
                    if (dj) {
                        t[j] = (byte) (t[j] - sb[j] + z3Field.createOne());
                        t[j] = z3Field.mod(t[j]);
                    } else {
                        t[j] = z3Field.sub(z3Field.createZero(), sb[j]);
                    }
                }
                return matrixB.leftMul(t);
            })
            .toArray(byte[][]::new);
        stopWatch.stop();
        long qTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 4, 4, qTime);

        return qs;
    }
}
