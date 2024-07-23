package edu.alibaba.mpc4j.s2pc.aby.basics.z2.bea91;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.AbstractZ2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.bea91.Bea91Z2cPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Z2Triple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenParty;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Bea91 Z2 circuit sender.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class Bea91Z2cSender extends AbstractZ2cParty {
    /**
     * multiplication triple generation sender
     */
    private final Z2TripleGenParty z2TripleGenSender;

    public Bea91Z2cSender(Rpc senderRpc, Party receiverParty, Bea91Z2cConfig config) {
        super(Bea91Z2cPtoDesc.getInstance(), senderRpc, receiverParty, config);
        z2TripleGenSender = Z2TripleGenFactory.createSender(senderRpc, receiverParty, config.getZ2TripleGenConfig());
        addSubPto(z2TripleGenSender);
    }

    public Bea91Z2cSender(Rpc senderRpc, Party receiverParty, Party aiderParty, Bea91Z2cConfig config) {
        super(Bea91Z2cPtoDesc.getInstance(), senderRpc, receiverParty, config);
        z2TripleGenSender = Z2TripleGenFactory.createSender(senderRpc, receiverParty, aiderParty, config.getZ2TripleGenConfig());
        addSubPto(z2TripleGenSender);
    }

    @Override
    public void init(int expectTotalNum) throws MpcAbortException {
        setInitInput(expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        z2TripleGenSender.init(expectTotalNum);
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
            Z2Triple triple = z2TripleGenSender.generate(bitNum);
            stopWatch.stop();
            long mtgTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 3, mtgTime, "and (gen. triples)");

            // compute e0 and f0
            stopWatch.start();
            byte[] a0 = triple.getA();
            byte[] b0 = triple.getB();
            byte[] c0 = triple.getC();
            // e0 = x0 ⊕ a0
            byte[] e0 = BytesUtils.xor(x0.getBitVector().getBytes(), a0);
            // f0 = y0 ⊕ b0
            byte[] f0 = BytesUtils.xor(y0.getBitVector().getBytes(), b0);
            List<byte[]> e0f0Payload = new LinkedList<>();
            e0f0Payload.add(e0);
            e0f0Payload.add(f0);
            sendOtherPartyPayload(PtoStep.SENDER_SEND_E0_F0.ordinal(), e0f0Payload);
            stopWatch.stop();
            long e0f0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, e0f0Time, "and (open e/f)");

            List<byte[]> e1f1Payload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_E1_F1.ordinal());

            stopWatch.start();
            MpcAbortPreconditions.checkArgument(e1f1Payload.size() == 2);
            byte[] e1 = e1f1Payload.remove(0);
            byte[] f1 = e1f1Payload.remove(0);
            // e = (e0 ⊕ e1)
            byte[] z0 = BytesUtils.xor(e0, e1);
            // f = (f0 ⊕ f1)
            byte[] f = BytesUtils.xor(f0, f1);
            // z0 = (e ☉ b0) ⊕ (f ☉ a0) ⊕ c0
            BytesUtils.andi(z0, b0);
            BytesUtils.andi(f, a0);
            BytesUtils.xori(z0, f);
            BytesUtils.xori(z0, c0);
            SquareZ2Vector z0SquareVector = SquareZ2Vector.create(bitNum, z0, false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, z0Time, "and (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "and");
            return z0SquareVector;
        }
    }
}
