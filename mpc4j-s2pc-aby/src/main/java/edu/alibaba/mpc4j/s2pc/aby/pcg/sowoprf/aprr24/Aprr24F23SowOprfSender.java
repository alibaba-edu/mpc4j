package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3Utils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.AbstractF23SowOprfSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F23Wprf;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24.Aprr24F23SowOprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotSender;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * APRR24 (F2, F3)-sowOPRF sender.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
public class Aprr24F23SowOprfSender extends AbstractF23SowOprfSender {
    /**
     * core COT receiver, used to generate PRG seeds.
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * COT sender, used for OPRF evaluation.
     */
    private final CotSender cotSender;
    /**
     * pre-computed COT sender
     */
    private final PreCotSender preCotSender;
    /**
     * Let G′_i (i ∈ [0, n), n = 4λ) denote a stateful PRNG with F_3 output held by P0 with seed σ_{i,k_i}.
     */
    private SecureRandom[] gbArray;

    public Aprr24F23SowOprfSender(Rpc senderRpc, Party receiverParty, Aprr24F23SowOprfConfig config) {
        super(Aprr24F23SowOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        cotSender = CotFactory.createSender(senderRpc, receiverParty, config.getCotConfig());
        addSubPto(cotSender);
        preCotSender = PreCotFactory.createSender(senderRpc, receiverParty, config.getPreCotConfig());
        addSubPto(preCotSender);
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
        coreCotReceiver.init();
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        assert expectBatchSize != 0;
        if (expectBatchSize > 0) {
            cotSender.init(delta, expectBatchSize * F23Wprf.M);
        } else {
            cotSender.init(delta);
        }
        preCotSender.init();
        stopWatch.stop();
        long initPtoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, initPtoTime);

        stopWatch.start();
        // The parties perform n random OTs where P0 is OT receiver with choice bit k_i.
        // P1 receives two random strings σ_{i,0}, σ_{i,1} ∈ {0,1}^λ and P0 receives σ_{i,k_i}.
        boolean[] binaryKey = BinaryUtils.byteArrayToBinary(key);
        assert binaryKey.length == F23Wprf.N;
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(binaryKey);
        RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfType.MMO, cotReceiverOutput);
        // Let G′_i denote a stateful PRNG with F_3 output held by P0 with seed σ_{i,k_i}.
        gbArray = IntStream.range(0, F23Wprf.N)
            .mapToObj(i -> {
                SecureRandom secureRandom = CommonUtils.createSeedSecureRandom();
                secureRandom.setSeed(rotReceiverOutput.getRb(i));
                return secureRandom;
            })
            .toArray(SecureRandom[]::new);
        long initParamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, initParamTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] oprf(int batchSize) throws MpcAbortException {
        setPtoInput(batchSize);
        return loopOprf();
    }

    @Override
    public byte[][] oprf(int batchSize, CotSenderOutput preCotSenderOutput) throws MpcAbortException {
        setPtoInput(batchSize, preCotSenderOutput);
        return loopOprf();
    }

    private byte[][] loopOprf() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        byte[][] ws;
        if (batchSize > Aprr24F23SowOprfPtoDesc.MAX_BATCH_SIZE) {
            int batchNum = (int) Math.ceil(batchSize * 1.0 / Aprr24F23SowOprfPtoDesc.MAX_BATCH_SIZE);
            ws = new byte[batchSize][];
            for (int i = 0; i < batchNum; i++) {
                int startIndex = i * Aprr24F23SowOprfPtoDesc.MAX_BATCH_SIZE;
                int endIndex = Math.min(startIndex + Aprr24F23SowOprfPtoDesc.MAX_BATCH_SIZE, batchSize);
                byte[][] subWs = innerOprf(endIndex - startIndex, i);
                System.arraycopy(subWs, 0, ws, startIndex, subWs.length);
            }
        } else {
            ws = innerOprf(batchSize, 0);
        }
        if (preCotSenderOutput != null) {
            assert preCotSenderOutput.getNum() == 0;
            preCotSenderOutput = null;
        }
        logPhaseInfo(PtoState.PTO_END);
        return ws;
    }

    private byte[][] innerOprf(int subBatchSize, int currentBatchIndex) throws MpcAbortException {
        stopWatch.start();
        // P0 computes g_i ← G′_i for i ∈ [n]
        byte[][] gs = new byte[subBatchSize][F23Wprf.N_BYTE_LENGTH];
        IntStream.range(0, subBatchSize).forEach(batchIndex -> {
            for (int i = 0; i < F23Wprf.N; i++) {
                BinaryUtils.setBoolean(gs[batchIndex], i, gbArray[i].nextBoolean());
            }
        });
        stopWatch.stop();
        long gsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 1, 4, gsTime);

        List<byte[]> fPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_F.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(fPayload.size() == subBatchSize);
        byte[][] fs = fPayload.toArray(byte[][]::new);
        // P0 computes v := A_2 ·_2 ((k ⊙ f) ⊕ g)
        IntStream vBatchIntStream = IntStream.range(0, subBatchSize);
        vBatchIntStream = parallel ? vBatchIntStream.parallel() : vBatchIntStream;
        byte[][] vs = vBatchIntStream.mapToObj(batchIndex -> {
            // k ⊙ f
            byte[] kfg2 = BytesUtils.and(key, fs[batchIndex]);
            // (k ⊙ f) ⊕ g
            BytesUtils.xori(kfg2, gs[batchIndex]);
            // v := A ·_2 ((k ⊙ f) ⊕ g)
            return matrixA.leftMultiply(kfg2);
        }).toArray(byte[][]::new);
        stopWatch.stop();
        long vTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 2, 4, vTime);

        stopWatch.start();
        // The parties preprocess/generate m random OTs where P_0 holds s ∈ F_3^{2×m}
        CotSenderOutput cotSenderOutput;
        if (preCotSenderOutput != null) {
            cotSenderOutput = preCotSenderOutput.split(subBatchSize * F23Wprf.M);
            cotSenderOutput = preCotSender.send(cotSenderOutput);
        } else {
            cotSenderOutput = cotSender.send(subBatchSize * F23Wprf.M);
        }
        RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfType.MMO, cotSenderOutput);
        IntStream sPairsIntStream = IntStream.range(0, subBatchSize);
        sPairsIntStream = parallel ? sPairsIntStream.parallel() : sPairsIntStream;
        byte[][][] sPairs = sPairsIntStream
            .mapToObj(batchIndex -> {
                byte[][] sPair = new byte[F23Wprf.M][2];
                int offset = batchIndex * F23Wprf.M;
                for (int j = 0; j < F23Wprf.M; j++) {
                    sPair[j][0] = z3Field.mod(rotSenderOutput.getR0(offset + j)[0]);
                    sPair[j][1] = z3Field.mod(rotSenderOutput.getR1(offset + j)[0]);
                }
                return sPair;
            })
            .toArray(byte[][][]::new);
        stopWatch.stop();
        long rotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 3, 4, rotTime);

        stopWatch.start();
        byte[][] ws = new byte[subBatchSize][];
        IntStream tBatchIntStream = IntStream.range(0, subBatchSize);
        tBatchIntStream = parallel ? tBatchIntStream.parallel() : tBatchIntStream;
        byte[][] ts = tBatchIntStream
            .mapToObj(batchIndex -> {
                byte[] byteV = new byte[F23Wprf.M];
                byte[] s0 = new byte[F23Wprf.M];
                byte[] s1 = new byte[F23Wprf.M];
                for (int j = 0; j < F23Wprf.M; j++) {
                    byteV[j] = BinaryUtils.getBoolean(vs[batchIndex], j) ? (byte) 1 : 0;
                    s0[j] = sPairs[batchIndex][j][0];
                    s1[j] = sPairs[batchIndex][j][1];
                }
                long[] longV = Z3Utils.compressToLongArray(byteV);
                long[] longS0 = Z3Utils.compressToLongArray(s0);
                long[] longS1 = Z3Utils.compressToLongArray(s1);
                // t := v -_3 s_0 +_3 s_1
                long[] longT = LongUtils.clone(longS0);
                Z3Utils.uncheckCompressLongNegi(longT);
                Z3Utils.uncheckCompressLongAddi(longT, longV);
                Z3Utils.uncheckCompressLongAddi(longT, longS1);
                // s0 := s0 + v
                Z3Utils.uncheckCompressLongAddi(longS0, longV);
                ws[batchIndex] = matrixB.leftCompressMul(longS0);
                return LongUtils.longArrayToByteArray(longT);
            })
            .toArray(byte[][]::new);
        List<byte[]> tPayload = Arrays.stream(ts).toList();
        sendOtherPartyPayload(PtoStep.SENDER_SEND_T.ordinal(), tPayload);
        stopWatch.stop();
        long wTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 4, 4, wTime);

        return ws;
    }
}
