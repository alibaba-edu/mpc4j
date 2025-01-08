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
 * Bea91 Zl circuit sender.
 *
 * @author Li Peng
 * @date 2024/7/23
 */
public class Bea91Zl64cSender extends AbstractZl64cParty {
    /**
     * multiplication triple generator
     */
    private final Zl64TripleGenParty zl64TripleGenSender;

    public Bea91Zl64cSender(Rpc senderRpc, Party receiverParty, Bea91Zl64cConfig config) {
        super(Bea91Zl64cPtoDesc.getInstance(), senderRpc, receiverParty, config);
        zl64TripleGenSender = Zl64TripleGenFactory.createSender(senderRpc, receiverParty, config.getZl64TripleGenConfig());
        addSubPto(zl64TripleGenSender);
    }

    @Override
    public void init(int maxL, int expectTotalNum) throws MpcAbortException {
        setInitInput(maxL, expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        zl64TripleGenSender.init(maxL, expectTotalNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZl64Vector shareOwn(Zl64Vector x0) {
        setShareOwnInput(x0);
        logPhaseInfo(PtoState.PTO_BEGIN, "send share");

        stopWatch.start();
        Zl64 zl64 = x0.getZl64();
        int byteL = zl64.getByteL();
        Zl64Vector x0Vector = Zl64Vector.createRandom(zl64, num, secureRandom);
        Zl64Vector x1Vector = x0.sub(x0Vector);
        List<byte[]> x1Payload = Arrays.stream(x1Vector.getElements())
            .mapToObj(element -> LongUtils.longToFixedByteArray(element, byteL))
            .collect(Collectors.toList());
        sendOtherPartyPayload(PtoStep.SENDER_SEND_INPUT_SHARE.ordinal(), x1Payload);
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, shareTime, "send share");

        logPhaseInfo(PtoState.PTO_END, "send share");
        return SquareZl64Vector.create(x0Vector, false);
    }

    @Override
    public SquareZl64Vector shareOther(Zl64 zl64, int num) throws MpcAbortException {
        setShareOtherInput(zl64, num);
        logPhaseInfo(PtoState.PTO_BEGIN, "receive share");

        List<byte[]> x0Payload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_INPUT_SHARE.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(x0Payload.size() == num);
        long[] x0Array = x0Payload.stream()
            .mapToLong(LongUtils::fixedByteArrayToLong)
            .toArray();
        Zl64Vector x0Vector = Zl64Vector.create(zl64, x0Array);
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, shareTime, "receive share");

        logPhaseInfo(PtoState.PTO_END, "receive share");
        return SquareZl64Vector.create(x0Vector, false);
    }

    @Override
    public Zl64Vector revealOwn(MpcZl64Vector x0) throws MpcAbortException {
        SquareZl64Vector x0SquareVector = (SquareZl64Vector) x0;
        setRevealOwnInput(x0SquareVector);
        if (x0.isPlain()) {
            return x0.getZl64Vector();
        } else {
            logPhaseInfo(PtoState.PTO_BEGIN, "receive share");

            List<byte[]> x1Payload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_OUTPUT_SHARE.ordinal());

            stopWatch.start();
            MpcAbortPreconditions.checkArgument(x1Payload.size() == num);
            Zl64 zl64 = x0.getZl64();
            long[] x1Array = x1Payload.stream()
                .mapToLong(LongUtils::fixedByteArrayToLong)
                .toArray();
            Zl64Vector x0Vector = x0.getZl64Vector();
            Zl64Vector x1Vector = Zl64Vector.create(zl64, x1Array);
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "receive share");

            logPhaseInfo(PtoState.PTO_END, "receive share");
            return x0Vector.add(x1Vector);
        }
    }

    @Override
    public void revealOther(MpcZl64Vector x0) {
        SquareZl64Vector x0SquareVector = (SquareZl64Vector) x0;
        setRevealOtherInput(x0SquareVector);
        if (!x0.isPlain()) {
            logPhaseInfo(PtoState.PTO_BEGIN, "send share");

            stopWatch.start();
            Zl64 zl64 = x0.getZl64();
            int byteL = zl64.getByteL();
            List<byte[]> x0Payload = Arrays.stream(x0.getZl64Vector().getElements())
                .mapToObj(element -> LongUtils.longToFixedByteArray(element, zl64.getByteL()))
                .collect(Collectors.toList());
            sendOtherPartyPayload(PtoStep.SENDER_SEND_OUTPUT_SHARE.ordinal(), x0Payload);
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "send share");

            logPhaseInfo(PtoState.PTO_END, "send share");
        }
    }

    @Override
    public SquareZl64Vector add(MpcZl64Vector x0, MpcZl64Vector y0) {
        SquareZl64Vector x0SquareVector = (SquareZl64Vector) x0;
        SquareZl64Vector y0SquareVector = (SquareZl64Vector) y0;
        setDyadicOperatorInput(x0SquareVector, y0SquareVector);
        if (x0.isPlain() && y0.isPlain()) {
            // x0 and y0 are plain vector, using plain add.
            Zl64Vector z0Vector = x0.getZl64Vector().add(y0.getZl64Vector());
            return SquareZl64Vector.create(z0Vector, true);
        } else if (x0.isPlain() || y0.isPlain()) {
            // x0 or y0 is plain vector, the sender does plain add.
            Zl64Vector z0Vector = x0.getZl64Vector().add(y0.getZl64Vector());
            return SquareZl64Vector.create(z0Vector, false);
        } else {
            // x0 and y0 are secret vector, using secret add.
            logPhaseInfo(PtoState.PTO_BEGIN, "add");

            stopWatch.start();
            Zl64Vector z0Vector = x0.getZl64Vector().add(y0.getZl64Vector());
            SquareZl64Vector z0SquareVector = SquareZl64Vector.create(z0Vector, false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, z0Time, "add (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "add");
            return z0SquareVector;
        }
    }

    @Override
    public SquareZl64Vector sub(MpcZl64Vector x0, MpcZl64Vector y0) {
        SquareZl64Vector x0SquareVector = (SquareZl64Vector) x0;
        SquareZl64Vector y0SquareVector = (SquareZl64Vector) y0;
        setDyadicOperatorInput(x0SquareVector, y0SquareVector);
        if (x0.isPlain() && y0.isPlain()) {
            // x0 and y0 are plain vector, using plain sub.
            Zl64Vector z0Vector = x0.getZl64Vector().sub(y0.getZl64Vector());
            return SquareZl64Vector.create(z0Vector, true);
        } else if (x0.isPlain() || y0.isPlain()) {
            // x0 or y0 is plain vector, the sender does plain sub.
            Zl64Vector z0Vector = x0.getZl64Vector().sub(y0.getZl64Vector());
            return SquareZl64Vector.create(z0Vector, false);
        } else {
            // x0 and y0 are secret vector, using secret sub.
            logPhaseInfo(PtoState.PTO_BEGIN, "sub");

            stopWatch.start();
            Zl64Vector z0Vector = x0.getZl64Vector().sub(y0.getZl64Vector());
            SquareZl64Vector z0SquareVector = SquareZl64Vector.create(z0Vector, false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, z0Time, "sub (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "sub");
            return z0SquareVector;
        }
    }

    @Override
    public SquareZl64Vector mul(MpcZl64Vector x0, MpcZl64Vector y0) throws MpcAbortException {
        SquareZl64Vector x0SquareVector = (SquareZl64Vector) x0;
        SquareZl64Vector y0SquareVector = (SquareZl64Vector) y0;
        setDyadicOperatorInput(x0SquareVector, y0SquareVector);
        if (x0.isPlain() && y0.isPlain()) {
            // x0 and y0 are plain vector, using plain mul.
            Zl64Vector z0Vector = x0.getZl64Vector().mul(y0.getZl64Vector());
            return SquareZl64Vector.create(z0Vector, true);
        } else if (x0.isPlain() || y0.isPlain()) {
            // x0 or y0 is plain vector, using plain mul.
            Zl64Vector z0Vector = x0.getZl64Vector().mul(y0.getZl64Vector());
            return SquareZl64Vector.create(z0Vector, false);
        } else {
            // x0 and y0 are secret vector, using secret mul.
            logPhaseInfo(PtoState.PTO_BEGIN, "mul");

            stopWatch.start();
            Zl64 zl64 = x0.getZl64();
            int byteL = zl64.getByteL();
            Zl64Triple triple = zl64TripleGenSender.generate(zl64, num);
            stopWatch.stop();
            long mtgTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 3, mtgTime, "mul (gen. triples)");

            // compute e0 and f0
            stopWatch.start();
            Zl64Vector a0 = Zl64Vector.create(zl64, triple.getA());
            Zl64Vector b0 = Zl64Vector.create(zl64, triple.getB());
            Zl64Vector c0 = Zl64Vector.create(zl64, triple.getC());
            // e0 = x0 - a0
            Zl64Vector e0 = x0.getZl64Vector().sub(a0);
            // f0 = y0 - b0
            Zl64Vector f0 = y0.getZl64Vector().sub(b0);
            List<byte[]> e0f0Payload = Arrays.stream(e0.getElements())
                .mapToObj(element -> LongUtils.longToFixedByteArray(element, byteL))
                .collect(Collectors.toList());
            List<byte[]> f0Payload = Arrays.stream(f0.getElements())
                .mapToObj(element -> LongUtils.longToFixedByteArray(element, byteL))
                .toList();
            e0f0Payload.addAll(f0Payload);
            sendOtherPartyPayload(PtoStep.SENDER_SEND_E0_F0.ordinal(), e0f0Payload);
            stopWatch.stop();
            long e0f0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, e0f0Time, "mul (open e/f)");

            List<byte[]> e1f1Payload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_E1_F1.ordinal());

            stopWatch.start();
            MpcAbortPreconditions.checkArgument(e1f1Payload.size() == 2 * num);
            long[] e1f1 = e1f1Payload.stream()
                .mapToLong(LongUtils::fixedByteArrayToLong)
                .toArray();
            long[] e1 = new long[num];
            System.arraycopy(e1f1, 0, e1, 0, num);
            long[] f1 = new long[num];
            System.arraycopy(e1f1, num, f1, 0, num);
            // e = (e0 + e1)
            Zl64Vector z0 = e0.add(Zl64Vector.create(zl64, e1));
            // f = (f0 + f1)
            Zl64Vector f = f0.add(Zl64Vector.create(zl64, f1));
            // z0 = (e * b0) + (f * a0) + c0
            z0.muli(b0);
            f.muli(a0);
            z0.addi(f);
            z0.addi(c0);
            SquareZl64Vector z0SquareVector = SquareZl64Vector.create(zl64, z0.getElements(), false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, z0Time, "mul (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "mul");
            return z0SquareVector;
        }
    }
}
