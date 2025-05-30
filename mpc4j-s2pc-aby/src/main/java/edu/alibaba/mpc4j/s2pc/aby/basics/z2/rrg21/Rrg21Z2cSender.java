package edu.alibaba.mpc4j.s2pc.aby.basics.z2.rrg21;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.AbstractZ2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.rrg21.Rrg21Z2cPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * RRG+21 Z2 circuit sender.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class Rrg21Z2cSender extends AbstractZ2cParty {
    /**
     * COT sender
     */
    private final CotSender cotSender;
    /**
     * COT receiver
     */
    private final CotReceiver cotReceiver;
    /**
     * -s0 vector
     */
    private BitVector negS0Vector;
    /**
     * t0 vector
     */
    private BitVector t0Vector;

    public Rrg21Z2cSender(Rpc senderRpc, Party receiverParty, Rrg21Z2cConfig config) {
        super(Rrg21Z2cPtoDesc.getInstance(), senderRpc, receiverParty, config);
        CotConfig cotConfig = config.getCotConfig();
        cotSender = CotFactory.createSender(senderRpc, receiverParty, cotConfig);
        addSubPto(cotSender);
        cotReceiver = CotFactory.createReceiver(senderRpc, receiverParty, cotConfig);
        addSubPto(cotReceiver);
    }

    @Override
    public void init(int expectTotalNum) throws MpcAbortException {
        setInitInput(expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = BlockUtils.randomBlock(secureRandom);
        // since storing many COT outputs would lead to memory exception, here we generate COT when necessary
        cotSender.init(delta, expectTotalNum);
        cotReceiver.init(expectTotalNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector shareOwn(BitVector x0) {
        setShareOwnInput(x0);
        logPhaseInfo(PtoState.PTO_BEGIN, "send share");

        stopWatch.start();
        BitVector x0Vector = BitVectorFactory.createRandom(bitNum, secureRandom);
        BitVector x1Vector = x0.xor(x0Vector);
        List<byte[]> x1Payload = Collections.singletonList(x1Vector.getBytes());
        sendOtherPartyPayload(PtoStep.SENDER_SEND_INPUT_SHARE.ordinal(), x1Payload);
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, shareTime, "send share");

        logPhaseInfo(PtoState.PTO_END, "send share");
        return SquareZ2Vector.create(x0Vector, false);
    }

    @Override
    public SquareZ2Vector shareOther(int bitNum) throws MpcAbortException {
        setShareOtherInput(bitNum);
        logPhaseInfo(PtoState.PTO_BEGIN, "receive share");

        List<byte[]> x0Payload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_INPUT_SHARE.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(x0Payload.size() == 1);
        BitVector x0Vector = BitVectorFactory.create(bitNum, x0Payload.get(0));
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, shareTime, "receive share");

        logPhaseInfo(PtoState.PTO_END, "receive share");
        return SquareZ2Vector.create(x0Vector, false);
    }

    @Override
    public BitVector revealOwn(MpcZ2Vector x0) throws MpcAbortException {
        SquareZ2Vector x0SquareVector = (SquareZ2Vector) x0;
        setRevealOwnInput(x0SquareVector);
        if (x0.isPlain()) {
            return x0.getBitVector();
        } else {
            logPhaseInfo(PtoState.PTO_BEGIN, "receive share");

            List<byte[]> x1Payload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_OUTPUT_SHARE.ordinal());

            stopWatch.start();
            MpcAbortPreconditions.checkArgument(x1Payload.size() == 1);
            BitVector x0Vector = x0.getBitVector();
            BitVector x1Vector = BitVectorFactory.create(bitNum, x1Payload.get(0));
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "receive share");

            logPhaseInfo(PtoState.PTO_END, "receive share");
            return x0Vector.xor(x1Vector);
        }
    }

    @Override
    public void revealOther(MpcZ2Vector x0) {
        SquareZ2Vector x0SquareVector = (SquareZ2Vector) x0;
        setRevealOtherInput(x0SquareVector);
        if (!x0.isPlain()) {
            logPhaseInfo(PtoState.PTO_BEGIN, "send share");

            stopWatch.start();
            List<byte[]> x0Payload = Collections.singletonList(x0.getBitVector().getBytes());
            sendOtherPartyPayload(PtoStep.SENDER_SEND_OUTPUT_SHARE.ordinal(), x0Payload);
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "send share");

            logPhaseInfo(PtoState.PTO_END, "send share");
        }
    }

    @Override
    public SquareZ2Vector xor(MpcZ2Vector x0, MpcZ2Vector y0) {
        SquareZ2Vector x0SquareVector = (SquareZ2Vector) x0;
        SquareZ2Vector y0SquareVector = (SquareZ2Vector) y0;
        setDyadicOperatorInput(x0SquareVector, y0SquareVector);
        if (x0.isPlain() && y0.isPlain()) {
            // x0 and y0 are plain bit vector, using plain XOR.
            BitVector z0Vector = x0.getBitVector().xor(y0.getBitVector());
            return SquareZ2Vector.create(z0Vector, true);
        } else if (x0.isPlain() || y0.isPlain()) {
            // x0 or y0 is plain bit vector, the sender does plain XOR.
            BitVector z0Vector = x0.getBitVector().xor(y0.getBitVector());
            return SquareZ2Vector.create(z0Vector, false);
        } else {
            // x0 and y0 are secret bit vector, using secret XOR.
            logPhaseInfo(PtoState.PTO_BEGIN, "xor");

            stopWatch.start();
            BitVector z0Vector = x0.getBitVector().xor(y0.getBitVector());
            SquareZ2Vector z0SquareVector = SquareZ2Vector.create(z0Vector, false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, z0Time, "xor (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "xor");
            return z0SquareVector;
        }
    }

    @Override
    public void xori(MpcZ2Vector x1, MpcZ2Vector y1) throws MpcAbortException {
        SquareZ2Vector x1SquareVector = (SquareZ2Vector) x1;
        SquareZ2Vector y1SquareVector = (SquareZ2Vector) y1;
        assert !(x1.isPlain() && (!y1.isPlain()));
        setDyadicOperatorInput(x1SquareVector, y1SquareVector);

        x1.getBitVector().xori(y1.getBitVector());
    }

    @Override
    public void noti(MpcZ2Vector xi) {
        xi.getBitVectors()[0].noti();
    }

    @Override
    public SquareZ2Vector[] setPublicValues(BitVector[] data) {
        return Arrays.stream(data).map(each -> SquareZ2Vector.create(each.copy(), false)).toArray(SquareZ2Vector[]::new);
    }

    @Override
    public SquareZ2Vector and(MpcZ2Vector x0, MpcZ2Vector y0) throws MpcAbortException {
        SquareZ2Vector x0SquareVector = (SquareZ2Vector) x0;
        SquareZ2Vector y0SquareVector = (SquareZ2Vector) y0;
        setDyadicOperatorInput(x0SquareVector, y0SquareVector);
        if (x0.isPlain() && y0.isPlain()) {
            // x0 and y0 are plain bit vector, using plain AND.
            BitVector z0Vector = x0.getBitVector().and(y0.getBitVector());
            return SquareZ2Vector.create(z0Vector, true);
        } else if (x0.isPlain() || y0.isPlain()) {
            // x0 or y0 is plain bit vector, using plain AND.
            BitVector z0Vector = x0.getBitVector().and(y0.getBitVector());
            return SquareZ2Vector.create(z0Vector, false);
        } else {
            // x0 and y0 are secret bit vector, using secret AND.
            logPhaseInfo(PtoState.PTO_BEGIN, "and");

            stopWatch.start();
            // P0 invokes an instance of COT, where P0 is the sender.
            CotSenderOutput cotSenderOutput = cotSender.send(bitNum);
            RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfFactory.CrhfType.MMO, cotSenderOutput);
            // P0 invokes an instance of COT, where P0 is the receiver with inputs x0.
            byte[] x0Bytes = x0.getBitVector().getBytes();
            boolean[] x0Binary = BinaryUtils.byteArrayToBinary(x0Bytes, bitNum);
            CotReceiverOutput cotReceiverOutput = cotReceiver.receive(x0Binary);
            RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfFactory.CrhfType.MMO, cotReceiverOutput);
            stopWatch.stop();
            long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 4, cotTime);

            stopWatch.start();
            List<byte[]> delta0Payload = generateDelta0(rotSenderOutput, y0SquareVector);
            sendOtherPartyPayload(PtoStep.SENDER_SEND_DELTA0.ordinal(), delta0Payload);
            stopWatch.stop();
            long delta0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 4, delta0Time);

            List<byte[]> delta1Payload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_DELTA1.ordinal());

            stopWatch.start();
            handleDelta1Payload(rotReceiverOutput, delta1Payload);
            stopWatch.stop();
            long delta1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 4, delta1Time);

            stopWatch.start();
            SquareZ2Vector z0SquareVector = generateZ0(x0SquareVector, y0SquareVector);
            negS0Vector = null;
            t0Vector = null;
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 4, 4, z0Time);

            logPhaseInfo(PtoState.PTO_END, "and");
            return z0SquareVector;
        }
    }

    private List<byte[]> generateDelta0(RotSenderOutput rotSenderOutput, SquareZ2Vector y0SquareVector) {
        BitVector y0Vector = y0SquareVector.getBitVector();
        BitVector s0Vector = BitVectorFactory.createZeros(bitNum);
        BitVector s1Vector = BitVectorFactory.createZeros(bitNum);
        // compute Δr, note that we cannot parallel execute the protocol
        IntStream.range(0, bitNum).forEach(index -> {
            s0Vector.set(index, rotSenderOutput.getR0(index)[0] % 2 == 1);
            s1Vector.set(index, rotSenderOutput.getR1(index)[0] % 2 == 1);
        });
        // Δ0 = Δ - Δr = Δ ⊕ Δr, where Δ = y0 − 2 * x0 * y0 = y0 ⊕ (0 ☉ x0 ☉ y0) = y0, hence Δ0 = y0 ⊕ Δr
        BitVector delta0Vector = s0Vector.xor(s1Vector);
        delta0Vector.xori(y0Vector);
        List<byte[]> delta0Payload = new LinkedList<>();
        delta0Payload.add(delta0Vector.getBytes());
        negS0Vector = s0Vector.not();
        return delta0Payload;
    }

    private void handleDelta1Payload(RotReceiverOutput rotReceiverOutput, List<byte[]> delta1Payload)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(delta1Payload.size() == 1);
        BitVector delta1Vector = BitVectorFactory.create(bitNum, delta1Payload.remove(0));
        t0Vector = BitVectorFactory.createZeros(bitNum);
        IntStream.range(0, bitNum).forEach(index -> {
            boolean x0 = rotReceiverOutput.getChoice(index);
            boolean t0 = rotReceiverOutput.getRb(index)[0] % 2 == 1;
            if (!x0) {
                t0Vector.set(index, t0);
            } else {
                t0Vector.set(index, t0 ^ delta1Vector.get(index));
            }
        });
    }

    private SquareZ2Vector generateZ0(SquareZ2Vector x0SquareVector, SquareZ2Vector y0SquareVector) {
        BitVector x0Vector = x0SquareVector.getBitVector();
        BitVector y0Vector = y0SquareVector.getBitVector();
        BitVector z0Vector = BitVectorFactory.createZeros(bitNum);
        // x0 * y0 = x0 ☉ y0
        z0Vector.xori(x0Vector);
        z0Vector.andi(y0Vector);
        // x0 * y0 + x1 * (y0 − 2 * x0 * y0)
        z0Vector.xori(negS0Vector);
        // x0 * y0 + x0 * (y1 − 2 * x1 * y1)
        z0Vector.xori(t0Vector);

        return SquareZ2Vector.create(z0Vector, false);
    }

    @Override
    public SquareZ2Vector[] and(MpcZ2Vector f, MpcZ2Vector[] xiArray) throws MpcAbortException{
        SquareZ2Vector[] xis = Arrays.stream(xiArray).map(each -> (SquareZ2Vector) each).toArray(SquareZ2Vector[]::new);
        boolean xIsPlain = xiArray[0].isPlain();
        for (MpcZ2Vector mpcZ2Vector : xiArray) {
            MathPreconditions.checkEqual("f.bitNum()", "xiArray[i].bitNum()", f.bitNum(), mpcZ2Vector.bitNum());
            Preconditions.checkArgument(mpcZ2Vector.isPlain() == xIsPlain);
        }
        if(xIsPlain || f.isPlain()) {
            // xi and f are both plain
            SquareZ2Vector[] res = new SquareZ2Vector[xiArray.length];
            for(int i = 0; i < xiArray.length; i++) {
                res[i] = and(f, xiArray[i]);
            }
            return res;
        }else{
            // xi or yi is secret, ci is secret
            logPhaseInfo(PtoState.PTO_BEGIN, "and");

            stopWatch.start();
            // P1 invokes an instance of COT, where P1 is the sender.
            CotSenderOutput cotSenderOutput = cotSender.send(f.bitNum());
            // P1 invokes an instance of COT, where P1 is the receiver with inputs x1.
            byte[] fBytes = f.getBitVector().getBytes();
            boolean[] fBinary = BinaryUtils.byteArrayToBinary(fBytes, f.bitNum());
            CotReceiverOutput cotReceiverOutput = cotReceiver.receive(fBinary);
            stopWatch.stop();
            long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 3, cotTime);

            int targetDim = xis.length;

            stopWatch.start();
            List<byte[]> delta1Payload = new LinkedList<>();
            BitVector[][] otSendMask = handleOtSenderOutput(cotSenderOutput, targetDim);
            for (int i = 0; i < targetDim; i++) {
                otSendMask[1][i].xori(otSendMask[0][i]);
                otSendMask[1][i].xori(xis[i].getBitVector());
                delta1Payload.add(otSendMask[1][i].getBytes());
            }
            sendOtherPartyPayload(PtoStep.SENDER_SEND_DELTA0.ordinal(), delta1Payload);
            stopWatch.stop();
            long delta1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, delta1Time);

            stopWatch.start();
            BitVector[] otRecMask = handleOtReceiverOutput(cotReceiverOutput, targetDim);
            List<byte[]> delta0Payload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_DELTA1.ordinal());
            MpcAbortPreconditions.checkArgument(delta0Payload.size() == targetDim);
            IntStream intStream = parallel ? IntStream.range(0, targetDim).parallel() : IntStream.range(0, targetDim);
            SquareZ2Vector[] resVec = intStream.mapToObj(i -> {
                BitVector delta0Vector = BitVectorFactory.create(f.bitNum(), delta0Payload.get(i));
                delta0Vector.andi(f.getBitVector());
                delta0Vector.xori(otRecMask[i]);
                delta0Vector.xori(otSendMask[0][i]);
                delta0Vector.xori(f.getBitVector().and(xis[i].getBitVector()));
                return SquareZ2Vector.create(delta0Vector, false);
            }).toArray(SquareZ2Vector[]::new);
            stopWatch.stop();
            long delta0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();

            logStepInfo(PtoState.PTO_STEP, 3, 3, delta0Time);

            logPhaseInfo(PtoState.PTO_END, "and");
            return resVec;
        }
    }

    @Override
    public SquareZ2Vector[] mux(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray, MpcZ2Vector ci) throws MpcAbortException {
        SquareZ2Vector[] xis = Arrays.stream(xiArray).map(each -> (SquareZ2Vector) each).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] yis = Arrays.stream(yiArray).map(each -> (SquareZ2Vector) each).toArray(SquareZ2Vector[]::new);
        setDyadicOperatorInput(xis, yis);
        SquareZ2Vector c = (SquareZ2Vector) ci;

        SquareZ2Vector[] xorRes = IntStream.range(0, xis.length).mapToObj(i -> xor(xis[i], yis[i])).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] res = and(c, xorRes);
        for (int i = 0; i < xis.length; i++) {
            xori(res[i], xis[i]);
        }
        return res;
    }
}
