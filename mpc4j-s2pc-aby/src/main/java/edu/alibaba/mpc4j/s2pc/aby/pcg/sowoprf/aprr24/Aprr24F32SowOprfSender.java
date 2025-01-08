package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.AbstractF32SowOprfSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32Wprf;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24.Aprr24F32SowOprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Party;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * APRR24 (F3, F2)-sowOPRF sender.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public class Aprr24F32SowOprfSender extends AbstractF32SowOprfSender {
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * F_3 -> F_2 modulus conversion sender
     */
    private final Conv32Party conv32Sender;
    /**
     * Let G′_i (i ∈ [0, n), n = 4λ) denote a stateful PRNG with F_3 output held by P0 with seed σ_{i,k_i}.
     */
    private SecureRandom[] gbArray;

    public Aprr24F32SowOprfSender(Rpc senderRpc, Party receiverParty, Aprr24F32SowOprfConfig config) {
        super(Aprr24F32SowOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        conv32Sender = Conv32Factory.createSender(senderRpc, receiverParty, config.getConv32Config());
        addSubPto(conv32Sender);
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
        // The parties run the setup for Conv32 for m conversions.
        assert expectBatchSize != 0;
        if (expectBatchSize > 0) {
            conv32Sender.init(expectBatchSize * F32Wprf.M);
        } else {
            conv32Sender.init();
        }
        stopWatch.stop();
        long initPtoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, initPtoTime);

        stopWatch.start();
        // The parties perform n random OTs where P0 is OT receiver with choice bit k_i.
        // P1 receives two random strings σ_{i,0}, σ_{i,1} ∈ {0,1}^λ and P0 receives σ_{i,k_i}.
        boolean[] binaryKey = BinaryUtils.byteArrayToBinary(key);
        assert binaryKey.length == F32Wprf.N;
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(binaryKey);
        RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfType.MMO, cotReceiverOutput);
        // Let G′_i denote a stateful PRNG with F_3 output held by P0 with seed σ_{i,k_i}.

        gbArray = IntStream.range(0, F32Wprf.N)
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
        logPhaseInfo(PtoState.PTO_BEGIN);

        byte[][] s0s;
        if (batchSize > Aprr24F32SowOprfPtoDesc.MAX_BATCH_SIZE) {
            int batchNum = (int) Math.ceil(batchSize * 1.0 / Aprr24F32SowOprfPtoDesc.MAX_BATCH_SIZE);
            s0s = new byte[batchSize][];
            for (int i = 0; i < batchNum; i++) {
                int startIndex = i * Aprr24F32SowOprfPtoDesc.MAX_BATCH_SIZE;
                int endIndex = Math.min(startIndex + Aprr24F32SowOprfPtoDesc.MAX_BATCH_SIZE, batchSize);
                byte[][] tmpResult = innerOprf(endIndex - startIndex, i);
                System.arraycopy(tmpResult, 0, s0s, startIndex, tmpResult.length);
            }
        } else {
            s0s = innerOprf(batchSize, 0);
        }

        logPhaseInfo(PtoState.PTO_END);
        return s0s;
    }

    private byte[][] innerOprf(int subBatchSize, int currentBatchIndex) throws MpcAbortException {
        stopWatch.start();
        // P0 computes t_i ← G′_i for i ∈ [n]
        byte[][] ts = new byte[subBatchSize][F32Wprf.N];
        IntStream bitIntStream = IntStream.range(0, F32Wprf.N);
        bitIntStream = parallel ? bitIntStream.parallel() : bitIntStream;
        bitIntStream.forEach(i -> {
            for (int batchIndex = 0; batchIndex < subBatchSize; batchIndex++) {
                ts[batchIndex][i] = z3Field.createRandom(gbArray[i]);
            }
        });
        stopWatch.stop();
        long tsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 1, 4, tsTime);

        List<byte[]> fPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_F.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(fPayload.size() == subBatchSize);
        Stream<byte[]> fPayloadStream = parallel ? fPayload.stream().parallel() : fPayload.stream();
        byte[][] fs = fPayloadStream.map(each -> SerializeUtils.decompressL2(each, F32Wprf.N)).toArray(byte[][]::new);
        // P0 computes w_0 := A ·_3 ((k ⊙_3 f) +_3 t)
        // but actually, the correct one is: if k = 1, then A ·_3 ((k ⊙_3 f) +_3 t), else A ·_3 ((k ⊙_3 f) -_3 t)
        byte[][] w0s = new byte[subBatchSize][F32Wprf.M];
        IntStream batchIntStream = IntStream.range(0, subBatchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        batchIntStream.forEach(batchIndex -> {
            byte[] kft3 = new byte[F32Wprf.N];
            for (int i = 0; i < F32Wprf.N; i++) {
                boolean k1 = BinaryUtils.getBoolean(key, i);
                // k ⊙_3 f
                kft3[i] = k1 ? fs[batchIndex][i] : 0;
                // if k = 1, then ((k ⊙_3 f) +_3 t), else ((k ⊙_3 f) -_3 t)
                kft3[i] = k1 ? z3Field.add(kft3[i], ts[batchIndex][i]) : z3Field.sub(kft3[i], ts[batchIndex][i]);
            }
            // if k = 1, then A ·_3 ((k ⊙_3 f) +_3 t), else A ·_3 ((k ⊙_3 f) -_3 t)
            w0s[batchIndex] = matrixA.leftMul(kft3);
        });
        stopWatch.stop();
        long w0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 2, 4, w0Time);

        stopWatch.start();
        // P0, P1 invoke Conv32 with w_i as the input for P_i. Let v0 be the output for P0.
        byte[] w0 = new byte[subBatchSize * F32Wprf.M];
        for (int batchIndex = 0; batchIndex < subBatchSize; batchIndex++) {
            System.arraycopy(w0s[batchIndex], 0, w0, batchIndex * F32Wprf.M, F32Wprf.M);
        }
        byte[] v0 = conv32Sender.conv(w0);
        stopWatch.stop();
        long v0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 3, 4, v0Time, "P0, P1 invoke Conv32");

        stopWatch.start();
        // P0 outputs B · v0.
        int byteM = F32Wprf.M / Byte.SIZE;
        byte[][] v0s = new byte[subBatchSize][byteM];
        for (int batchIndex = 0; batchIndex < subBatchSize; batchIndex++) {
            System.arraycopy(v0, batchIndex * byteM, v0s[batchIndex], 0, byteM);
        }
        batchIntStream = IntStream.range(0, subBatchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        byte[][] s0s = batchIntStream
            .mapToObj(batchIndex -> matrixB.leftMultiply(v0s[batchIndex]))
            .toArray(byte[][]::new);
        stopWatch.stop();
        long s0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 4, 4, s0Time);

        return s0s;
    }
}
