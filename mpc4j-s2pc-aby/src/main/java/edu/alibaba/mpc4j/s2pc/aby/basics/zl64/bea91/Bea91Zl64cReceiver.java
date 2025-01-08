package edu.alibaba.mpc4j.s2pc.aby.basics.zl64.bea91;

import edu.alibaba.mpc4j.common.circuit.zl64.MpcZl64Vector;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl64.AbstractZl64cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl64.SquareZl64Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl64.bea91.Bea91Zl64cPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Zl64Triple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.Zl64TripleGenFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.Zl64TripleGenParty;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Bea91 Zl circuit receiver.
 *
 * @author Li Peng
 * @date 2024/7/23
 */
public class Bea91Zl64cReceiver extends AbstractZl64cParty {
    /**
     * Zl triple generation receiver
     */
    private final Zl64TripleGenParty zl64TripleGenReceiver;

    public Bea91Zl64cReceiver(Rpc receiverRpc, Party senderParty, Bea91Zl64cConfig config) {
        super(Bea91Zl64cPtoDesc.getInstance(), receiverRpc, senderParty, config);
        zl64TripleGenReceiver = Zl64TripleGenFactory.createReceiver(receiverRpc, senderParty, config.getZl64TripleGenConfig());
        addSubPto(zl64TripleGenReceiver);
    }

    @Override
    public void init(int maxL, int expectTotalNum) throws MpcAbortException {
        setInitInput(maxL, expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        zl64TripleGenReceiver.init(maxL, expectTotalNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZl64Vector shareOwn(Zl64Vector x1) {
        setShareOwnInput(x1);
        logPhaseInfo(PtoState.PTO_BEGIN, "send share");

        stopWatch.start();
        Zl64 zl64 = x1.getZl64();
        int byteL = zl64.getByteL();
        Zl64Vector x1Vector = Zl64Vector.createRandom(zl64, num, secureRandom);
        Zl64Vector x0Vector = x1.sub(x1Vector);
        List<byte[]> x0Payload = Arrays.stream(x0Vector.getElements())
            .mapToObj(element -> LongUtils.longToFixedByteArray(element, byteL))
            .collect(Collectors.toList());
        sendOtherPartyPayload(PtoStep.RECEIVER_SEND_INPUT_SHARE.ordinal(), x0Payload);
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, shareTime, "send share");

        logPhaseInfo(PtoState.PTO_END, "send share");
        return SquareZl64Vector.create(x1Vector, false);
    }

    @Override
    public SquareZl64Vector shareOther(Zl64 zl64, int num) throws MpcAbortException {
        setShareOtherInput(zl64, num);
        logPhaseInfo(PtoState.PTO_BEGIN, "receive share");

        List<byte[]> x1Payload = receiveOtherPartyPayload(PtoStep.SENDER_SEND_INPUT_SHARE.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(x1Payload.size() == num);
        long[] x1Array = x1Payload.stream()
            .mapToLong(LongUtils::fixedByteArrayToLong)
            .toArray();
        Zl64Vector x1Vector = Zl64Vector.create(zl64, x1Array);
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, shareTime, "receive share");

        logPhaseInfo(PtoState.PTO_END, "receive share");
        return SquareZl64Vector.create(x1Vector, false);
    }

    @Override
    public Zl64Vector revealOwn(MpcZl64Vector x1) throws MpcAbortException {
        SquareZl64Vector x1SquareVector = (SquareZl64Vector) x1;
        setRevealOwnInput(x1SquareVector);
        if (x1.isPlain()) {
            return x1.getZl64Vector();
        } else {
            logPhaseInfo(PtoState.PTO_BEGIN, "receive share");

            List<byte[]> x0Payload = receiveOtherPartyPayload(PtoStep.SENDER_SEND_OUTPUT_SHARE.ordinal());

            stopWatch.start();
            MpcAbortPreconditions.checkArgument(x0Payload.size() == num);
            Zl64 zl64 = x1.getZl64();
            long[] x0Array = x0Payload.stream()
                .mapToLong(LongUtils::fixedByteArrayToLong)
                .toArray();
            Zl64Vector x0Vector = Zl64Vector.create(zl64, x0Array);
            Zl64Vector x1Vector = x1.getZl64Vector();
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "receive share");

            logPhaseInfo(PtoState.PTO_END, "receive share");
            return x0Vector.add(x1Vector);
        }
    }

    @Override
    public void revealOther(MpcZl64Vector x1) {
        SquareZl64Vector x1SquareVector = (SquareZl64Vector) x1;
        setRevealOtherInput(x1SquareVector);
        if (!x1.isPlain()) {
            logPhaseInfo(PtoState.PTO_BEGIN, "send share");

            stopWatch.start();
            Zl64 zl64 = x1.getZl64();
            int byteL = zl64.getByteL();
            List<byte[]> x1Payload = Arrays.stream(x1.getZl64Vector().getElements())
                .mapToObj(element -> LongUtils.longToFixedByteArray(element, byteL))
                .collect(Collectors.toList());
            sendOtherPartyPayload(PtoStep.RECEIVER_SEND_OUTPUT_SHARE.ordinal(), x1Payload);
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "send share");

            logPhaseInfo(PtoState.PTO_END, "send share");
        }
    }

    @Override
    public SquareZl64Vector add(MpcZl64Vector x1, MpcZl64Vector y1) {
        SquareZl64Vector x1SquareVector = (SquareZl64Vector) x1;
        SquareZl64Vector y1SquareVector = (SquareZl64Vector) y1;
        setDyadicOperatorInput(x1SquareVector, y1SquareVector);

        if (x1.isPlain() && y1.isPlain()) {
            // x1 and y1 are plain vector, using plain add.
            Zl64Vector z1Vector = x1.getZl64Vector().add(y1.getZl64Vector());
            return SquareZl64Vector.create(z1Vector, true);
        } else if (x1.isPlain()) {
            // x1 is plain vector, y1 is secret vector, the receiver copies y1
            return y1SquareVector.copy();
        } else if (y1.isPlain()) {
            // x1 is secret vector, y1 is plain vector, the receiver copies x1
            return x1SquareVector.copy();
        } else {
            // x1 and y1 are secret vectors, using secret add.
            logPhaseInfo(PtoState.PTO_BEGIN, "add");

            stopWatch.start();
            Zl64Vector z1Vector = x1.getZl64Vector().add(y1.getZl64Vector());
            SquareZl64Vector z1SquareVector = SquareZl64Vector.create(z1Vector, false);
            stopWatch.stop();
            long z1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, z1Time, "add (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "add");
            return z1SquareVector;
        }
    }

    @Override
    public SquareZl64Vector sub(MpcZl64Vector x1, MpcZl64Vector y1) {
        SquareZl64Vector x1SquareVector = (SquareZl64Vector) x1;
        SquareZl64Vector y1SquareVector = (SquareZl64Vector) y1;
        setDyadicOperatorInput(x1SquareVector, y1SquareVector);

        if (x1.isPlain() && y1.isPlain()) {
            // x1 and y1 are plain vector, using plain sub.
            Zl64Vector z1Vector = x1.getZl64Vector().sub(y1.getZl64Vector());
            return SquareZl64Vector.create(z1Vector, true);
        } else if (x1.isPlain()) {
            // x1 is plain vector, y1 is secret vector, the receiver computes 0 - y1
            Zl64 zl64 = x1.getZl64();
            Zl64Vector z1Vector = Zl64Vector.createZeros(zl64, num).sub(y1.getZl64Vector());
            return SquareZl64Vector.create(z1Vector, false);
        } else if (y1.isPlain()) {
            // x1 is secret vector, y1 is plain vector, the receiver copies x1
            return x1SquareVector.copy();
        } else {
            // x1 and y1 are secret vectors, using secret sub.
            logPhaseInfo(PtoState.PTO_BEGIN, "sub");

            stopWatch.start();
            Zl64Vector z1Vector = x1.getZl64Vector().sub(y1.getZl64Vector());
            SquareZl64Vector z1SquareVector = SquareZl64Vector.create(z1Vector, false);
            stopWatch.stop();
            long z1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, z1Time, "sub (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "sub");
            return z1SquareVector;
        }
    }

    @Override
    public SquareZl64Vector mul(MpcZl64Vector x1, MpcZl64Vector y1) throws MpcAbortException {
        SquareZl64Vector x1SquareVector = (SquareZl64Vector) x1;
        SquareZl64Vector y1SquareVector = (SquareZl64Vector) y1;
        setDyadicOperatorInput(x1SquareVector, y1SquareVector);

        if (x1.isPlain() && y1.isPlain()) {
            // x1 and y1 are plain vectors, using plain mul.
            Zl64Vector z1Vector = x1.getZl64Vector().mul(y1.getZl64Vector());
            return SquareZl64Vector.create(z1Vector, true);
        } else if (x1.isPlain() || y1.isPlain()) {
            // x1 or y1 is plain vector, using plain mul.
            Zl64Vector z164Vector = x1.getZl64Vector().mul(y1.getZl64Vector());
            return SquareZl64Vector.create(z164Vector, false);
        } else {
            // x1 and y1 are secret vectors, using secret mul.
            logPhaseInfo(PtoState.PTO_BEGIN, "mul");

            stopWatch.start();
            Zl64 zl64 = x1.getZl64();
            int byteL = zl64.getByteL();
            Zl64Triple triple = zl64TripleGenReceiver.generate(zl64, num);
            stopWatch.stop();
            long mtgTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 3, mtgTime, "and (gen. Boolean triples)");

            stopWatch.start();
            Zl64Vector a1 = Zl64Vector.create(zl64, triple.getA());
            Zl64Vector b1 = Zl64Vector.create(zl64, triple.getB());
            Zl64Vector c1 = Zl64Vector.create(zl64, triple.getC());
            // e1 = x1 - a1
            Zl64Vector e1 = x1.getZl64Vector().sub(a1);
            // f1 = y1 - b1
            Zl64Vector f1 = y1.getZl64Vector().sub(b1);
            List<byte[]> e1f1Payload = Arrays.stream(e1.getElements())
                .mapToObj(element -> LongUtils.longToFixedByteArray(element, byteL))
                .collect(Collectors.toList());
            List<byte[]> f1Payload = Arrays.stream(f1.getElements())
                .mapToObj(element -> LongUtils.longToFixedByteArray(element, byteL))
                .toList();
            e1f1Payload.addAll(f1Payload);
            sendOtherPartyPayload(PtoStep.RECEIVER_SEND_E1_F1.ordinal(), e1f1Payload);
            stopWatch.stop();
            long e1f1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, e1f1Time, "and (open e/f)");

            List<byte[]> e0f0Payload = receiveOtherPartyPayload(PtoStep.SENDER_SEND_E0_F0.ordinal());

            stopWatch.start();
            MpcAbortPreconditions.checkArgument(e0f0Payload.size() == 2 * num);
            long[] e0f0 = e0f0Payload.stream()
                .mapToLong(LongUtils::fixedByteArrayToLong)
                .toArray();
            long[] e0 = new long[num];
            System.arraycopy(e0f0, 0, e0, 0, num);
            long[] f0 = new long[num];
            System.arraycopy(e0f0, num, f0, 0, num);
            // e = (e0 + e1)
            Zl64Vector z1 = Zl64Vector.create(zl64, e0).add(e1);
            // f = (f0 + f1)
            Zl64Vector f = Zl64Vector.create(zl64, f0).add(f1);
            // z1 = (e * b1) + (f * a1) + c1 + (e * f)
            Zl64Vector ef = z1.mul(f);
            z1.muli(b1);
            f.muli(a1);
            z1.addi(f);
            z1.addi(c1);
            z1.addi(ef);
            SquareZl64Vector z1SquareVector = SquareZl64Vector.create(z1, false);
            stopWatch.stop();
            long z1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, z1Time, "mul (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "mul");
            return z1SquareVector;
        }
    }
}
