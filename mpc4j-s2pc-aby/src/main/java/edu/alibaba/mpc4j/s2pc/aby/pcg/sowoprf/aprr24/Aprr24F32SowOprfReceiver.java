package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.AbstractF32SowOprfReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32Wprf;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24.Aprr24F32SowOprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Party;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * APRR24 (F3, F2)-sowOPRF receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public class Aprr24F32SowOprfReceiver extends AbstractF32SowOprfReceiver {
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;
    /**
     * F_3 -> F_2 modulus conversion receiver
     */
    private final Conv32Party conv32Receiver;
    /**
     * Let G_{i,0} denote stateful PRNGs with F_3 output held by P1 with seeds σ_{i,0}.
     */
    private SecureRandom[] g0Array;
    /**
     * Let G_{i,1} denote stateful PRNGs with F_3 output held by P1 with seeds σ_{i,1}.
     */
    private SecureRandom[] g1Array;

    public Aprr24F32SowOprfReceiver(Rpc receiverRpc, Party senderParty, Aprr24F32SowOprfConfig config) {
        super(Aprr24F32SowOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotSender = CoreCotFactory.createSender(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        conv32Receiver = Conv32Factory.createReceiver(receiverRpc, senderParty, config.getConv32Config());
        addSubPto(conv32Receiver);
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
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        coreCotSender.init(delta);
        // The parties run the setup for Conv32 for m conversions.
        assert expectBatchSize != 0;
        if (expectBatchSize > 0) {
            conv32Receiver.init(expectBatchSize * F32Wprf.M);
        } else {
            conv32Receiver.init();
        }
        stopWatch.stop();
        long initPtoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, initPtoTime);

        stopWatch.start();
        // The parties perform n random OTs where P0 is OT receiver with choice bit k_i.
        // P1 receives two random strings σ_{i,0}, σ_{i,1} ∈ {0,1}^λ and P0 receives σ_{i,k_i}.
        CotSenderOutput cotSenderOutput = coreCotSender.send(F32Wprf.N);
        RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfType.MMO, cotSenderOutput);
        // Let G′_i denote a stateful PRNG with F_3 output held by P0 with seed σ_{i,k_i}.
        IntStream intStream = parallel ? IntStream.range(0, F32Wprf.N).parallel() : IntStream.range(0, F32Wprf.N);
        g0Array = new SecureRandom[F32Wprf.N];
        g1Array = new SecureRandom[F32Wprf.N];
        intStream.forEach(i -> {
            SecureRandom secureRandom0 = CommonUtils.createSeedSecureRandom();
            secureRandom0.setSeed(rotSenderOutput.getR0(i));
            g0Array[i] = secureRandom0;
            SecureRandom secureRandom1 = CommonUtils.createSeedSecureRandom();
            secureRandom1.setSeed(rotSenderOutput.getR1(i));
            g1Array[i] = secureRandom1;
        });
        stopWatch.stop();
        long initParamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, initParamTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] oprf(byte[][] inputs) throws MpcAbortException {
        setPtoInput(inputs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        byte[][] s1s;
        if (batchSize > Aprr24F32SowOprfPtoDesc.MAX_BATCH_SIZE) {
            int batchNum = (int) Math.ceil(batchSize * 1.0 / Aprr24F32SowOprfPtoDesc.MAX_BATCH_SIZE);
            s1s = new byte[batchSize][];
            for (int i = 0; i < batchNum; i++) {
                int startIndex = i * Aprr24F32SowOprfPtoDesc.MAX_BATCH_SIZE;
                int endIndex = Math.min(startIndex + Aprr24F32SowOprfPtoDesc.MAX_BATCH_SIZE, batchSize);
                byte[][] tmpInput = Arrays.copyOfRange(inputs, startIndex, endIndex);
                byte[][] tmpResult = innerOprf(tmpInput, i);
                System.arraycopy(tmpResult, 0, s1s, startIndex, tmpResult.length);
            }
        } else {
            s1s = innerOprf(inputs, 0);
        }

        logPhaseInfo(PtoState.PTO_END);
        return s1s;
    }

    private byte[][] innerOprf(byte[][] inputs, int currentBatchIndex) throws MpcAbortException {
        int singleBatchSize = inputs.length;
        stopWatch.start();
        // P1 computes h_{0,i} ← G_{i,0} for i ∈ [n], h_{1,i} ← G_{i,1} for i ∈ [n]
        byte[][] h0s = new byte[singleBatchSize][F32Wprf.N];
        byte[][] h1s = new byte[singleBatchSize][F32Wprf.N];
        IntStream bitIntStream = IntStream.range(0, F32Wprf.N);
        bitIntStream = parallel ? bitIntStream.parallel() : bitIntStream;
        bitIntStream.forEach(i -> {
            for (int batchIndex = 0; batchIndex < singleBatchSize; batchIndex++) {
                h0s[batchIndex][i] = z3Field.createRandom(g0Array[i]);
                h1s[batchIndex][i] = z3Field.createRandom(g1Array[i]);
            }
        });
        stopWatch.stop();
        long hsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 1, 4, hsTime);

        stopWatch.start();
        // P0 computes f := x −_3 h0 −_3 h1
        byte[][] fs = new byte[singleBatchSize][];
        IntStream batchIntStream = IntStream.range(0, singleBatchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        batchIntStream.forEach(batchIndex -> {
            byte[] singleData = new byte[F32Wprf.N];
            for (int i = 0; i < F32Wprf.N; i++) {
                singleData[i] = z3Field.sub(inputs[batchIndex][i], h0s[batchIndex][i]);
                singleData[i] = z3Field.sub(singleData[i], h1s[batchIndex][i]);
            }
            fs[batchIndex] = SerializeUtils.compressL2(singleData);
        });
        List<byte[]> fPayload = Arrays.stream(fs).collect(Collectors.toList());
        sendOtherPartyPayload(PtoStep.RECEIVER_SEND_F.ordinal(), fPayload);
        // P1 computes w_1 := A ·_3 h_0
        byte[][] w1s = new byte[singleBatchSize][F32Wprf.M];
        batchIntStream = IntStream.range(0, singleBatchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        batchIntStream.forEach(batchIndex -> w1s[batchIndex] = matrixA.leftMul(h0s[batchIndex]));
        stopWatch.stop();
        long w1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 2, 4, w1Time);

        stopWatch.start();
        // P0, P1 invoke Conv32 with w_i as the input for P_i. Let v1 be the output for P1.
        byte[] w1 = new byte[singleBatchSize * F32Wprf.M];
        for (int batchIndex = 0; batchIndex < singleBatchSize; batchIndex++) {
            System.arraycopy(w1s[batchIndex], 0, w1, batchIndex * F32Wprf.M, F32Wprf.M);
        }
        byte[] v1 = conv32Receiver.conv(w1);
        stopWatch.stop();
        long v1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 3, 4, v1Time, "P0, P1 invoke Conv32");

        stopWatch.start();
        // P1 outputs B · v1.
        int byteM = F32Wprf.M / Byte.SIZE;
        byte[][] v1s = new byte[singleBatchSize][byteM];
        for (int batchIndex = 0; batchIndex < singleBatchSize; batchIndex++) {
            System.arraycopy(v1, batchIndex * byteM, v1s[batchIndex], 0, byteM);
        }
        batchIntStream = IntStream.range(0, singleBatchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        byte[][] s1s = batchIntStream
            .mapToObj(batchIndex -> matrixB.leftMultiply(v1s[batchIndex]))
            .toArray(byte[][]::new);
        stopWatch.stop();
        long s1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, currentBatchIndex + 1, 4, 4, s1Time);

        return s1s;
    }
}
