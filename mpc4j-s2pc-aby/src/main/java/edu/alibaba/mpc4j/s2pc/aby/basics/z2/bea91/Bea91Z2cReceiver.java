package edu.alibaba.mpc4j.s2pc.aby.basics.z2.bea91;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.AbstractZ2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.bea91.Bea91Z2cPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Bea91 Z2 circuit receiver.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class Bea91Z2cReceiver extends AbstractZ2cParty {
    /**
     * multiplication triple generation receiver
     */
    private final Z2MtgParty mtgReceiver;

    public Bea91Z2cReceiver(Rpc receiverRpc, Party senderParty, Bea91Z2cConfig config) {
        super(Bea91Z2cPtoDesc.getInstance(), receiverRpc, senderParty, config);
        mtgReceiver = Z2MtgFactory.createReceiver(receiverRpc, senderParty, config.getMtgConfig());
        addSubPto(mtgReceiver);
    }

    public Bea91Z2cReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, Bea91Z2cConfig config) {
        super(Bea91Z2cPtoDesc.getInstance(), receiverRpc, senderParty, config);
        mtgReceiver = Z2MtgFactory.createReceiver(receiverRpc, senderParty, aiderParty, config.getMtgConfig());
        addSubPto(mtgReceiver);
    }

    @Override
    public void init(long updateBitNum) throws MpcAbortException {
        setInitInput(updateBitNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        mtgReceiver.init((int) updateBitNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector shareOwn(BitVector x1) {
        setShareOwnInput(x1);
        logPhaseInfo(PtoState.PTO_BEGIN, "send share");

        stopWatch.start();
        BitVector x1Vector = BitVectorFactory.createRandom(bitNum, secureRandom);
        BitVector x0Vector = x1.xor(x1Vector);
        List<byte[]> x0Payload = Collections.singletonList(x0Vector.getBytes());
        DataPacketHeader x0Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_INPUT_SHARE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(x0Header, x0Payload));
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, shareTime, "send share");

        logPhaseInfo(PtoState.PTO_END, "send share");
        return SquareZ2Vector.create(x1Vector, false);
    }

    @Override
    public SquareZ2Vector shareOther(int bitNum) throws MpcAbortException {
        setShareOtherInput(bitNum);
        logPhaseInfo(PtoState.PTO_BEGIN, "receive share");

        stopWatch.start();
        DataPacketHeader x1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_INPUT_SHARE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> x1Payload = rpc.receive(x1Header).getPayload();
        MpcAbortPreconditions.checkArgument(x1Payload.size() == 1);
        BitVector x1Vector = BitVectorFactory.create(bitNum, x1Payload.get(0));
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, shareTime, "receive share");

        logPhaseInfo(PtoState.PTO_END, "receive share");
        return SquareZ2Vector.create(x1Vector, false);
    }

    @Override
    public BitVector revealOwn(MpcZ2Vector x1) throws MpcAbortException {
        SquareZ2Vector x1SquareVector = (SquareZ2Vector) x1;
        setRevealOwnInput(x1SquareVector);
        if (x1.isPlain()) {
            return x1.getBitVector();
        } else {
            logPhaseInfo(PtoState.PTO_BEGIN, "receive share");

            stopWatch.start();
            DataPacketHeader x0Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_OUTPUT_SHARE.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> x0Payload = rpc.receive(x0Header).getPayload();
            MpcAbortPreconditions.checkArgument(x0Payload.size() == 1);
            BitVector x0Vector = BitVectorFactory.create(bitNum, x0Payload.get(0));
            BitVector x1Vector = x1.getBitVector();
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "receive share");

            logPhaseInfo(PtoState.PTO_END, "receive share");
            return x0Vector.xor(x1Vector);
        }
    }

    @Override
    public void revealOther(MpcZ2Vector x1) {
        SquareZ2Vector x1SquareVector = (SquareZ2Vector) x1;
        setRevealOtherInput(x1SquareVector);
        if (!x1.isPlain()) {
            logPhaseInfo(PtoState.PTO_BEGIN, "send share");

            stopWatch.start();
            List<byte[]> x1Payload = Collections.singletonList(x1.getBitVector().getBytes());
            DataPacketHeader x1Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_OUTPUT_SHARE.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(x1Header, x1Payload));
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "send share");

            logPhaseInfo(PtoState.PTO_END, "send share");
        }
    }

    @Override
    public SquareZ2Vector xor(MpcZ2Vector x1, MpcZ2Vector y1) {
        SquareZ2Vector x1SquareVector = (SquareZ2Vector) x1;
        SquareZ2Vector y1SquareVector = (SquareZ2Vector) y1;
        setDyadicOperatorInput(x1SquareVector, y1SquareVector);

        if (x1.isPlain() && y1.isPlain()) {
            // x1 and y1 are plain bit vector, using plain XOR.
            BitVector z1Vector = x1.getBitVector().xor(y1.getBitVector());
            return SquareZ2Vector.create(z1Vector, true);
        } else if (x1.isPlain()) {
            // x1 is plain bit vector, y1 is secret bit vector, the receiver copies y1
            return y1SquareVector.copy();
        } else if (y1.isPlain()) {
            // x1 is secret bit vector, y1 is plain bit vector, the receiver copies x1
            return x1SquareVector.copy();
        } else {
            // x1 and y1 are secret bit vectors, using secret XOR.
            logPhaseInfo(PtoState.PTO_BEGIN, "xor");

            stopWatch.start();
            BitVector z1Vector = x1.getBitVector().xor(y1.getBitVector());
            SquareZ2Vector z1SquareVector = SquareZ2Vector.create(z1Vector, false);
            stopWatch.stop();
            long z1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, z1Time, "xor (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "xor");
            return z1SquareVector;
        }
    }

    @Override
    public void xori(MpcZ2Vector x1, MpcZ2Vector y1) throws MpcAbortException {
        SquareZ2Vector x1SquareVector = (SquareZ2Vector) x1;
        SquareZ2Vector y1SquareVector = (SquareZ2Vector) y1;
        assert !(x1.isPlain() && (!y1.isPlain()));
        setDyadicOperatorInput(x1SquareVector, y1SquareVector);

        if(x1.isPlain() || (!y1.isPlain())){
            x1.getBitVector().xori(y1.getBitVector());
        }
    }

    @Override
    public void noti(MpcZ2Vector xi) {
        if(xi.isPlain()){
            xi.getBitVectors()[0].noti();
        }
    }

    @Override
    public SquareZ2Vector[] setPublicValues(BitVector[] data) {
        return Arrays.stream(data).map(each ->
            SquareZ2Vector.create(BitVectorFactory.createZeros(each.bitNum()), false)).toArray(SquareZ2Vector[]::new);
    }

    @Override
    public SquareZ2Vector and(MpcZ2Vector x1, MpcZ2Vector y1) throws MpcAbortException {
        SquareZ2Vector x1SquareVector = (SquareZ2Vector) x1;
        SquareZ2Vector y1SquareVector = (SquareZ2Vector) y1;
        setDyadicOperatorInput(x1SquareVector, y1SquareVector);

        if (x1.isPlain() && y1.isPlain()) {
            // x1 and y1 are plain bit vectors, using plain AND.
            BitVector z1Vector = x1.getBitVector().and(y1.getBitVector());
            return SquareZ2Vector.create(z1Vector, true);
        } else if (x1.isPlain() || y1.isPlain()) {
            // x1 or y1 is plain bit vector, using plain AND.
            BitVector z1Vector = x1.getBitVector().and(y1.getBitVector());
            return SquareZ2Vector.create(z1Vector, false);
        } else {
            // x1 and y1 are secret bit vectors, using secret AND.
            logPhaseInfo(PtoState.PTO_BEGIN, "and");

            stopWatch.start();
            Z2Triple triple = mtgReceiver.generate(bitNum);
            stopWatch.stop();
            long mtgTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 3, mtgTime, "and (gen. Boolean triples)");

            stopWatch.start();
            byte[] a1 = triple.getA();
            byte[] b1 = triple.getB();
            byte[] c1 = triple.getC();
            // e1 = x1 ⊕ a1
            byte[] e1 = BytesUtils.xor(x1.getBitVector().getBytes(), a1);
            // f1 = y1 ⊕ b1
            byte[] f1 = BytesUtils.xor(y1.getBitVector().getBytes(), b1);
            List<byte[]> e1f1Payload = new LinkedList<>();
            e1f1Payload.add(e1);
            e1f1Payload.add(f1);
            DataPacketHeader e1f1Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_E1_F1.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(e1f1Header, e1f1Payload));
            stopWatch.stop();
            long e1f1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, e1f1Time, "and (open e/f)");

            stopWatch.start();
            DataPacketHeader e0f0Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_E0_F0.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> e0f0Payload = rpc.receive(e0f0Header).getPayload();
            MpcAbortPreconditions.checkArgument(e0f0Payload.size() == 2);
            byte[] e0 = e0f0Payload.remove(0);
            byte[] f0 = e0f0Payload.remove(0);
            // e = (e0 ⊕ e1)
            byte[] z1 = BytesUtils.xor(e0, e1);
            // f = (f0 ⊕ f1)
            byte[] f = BytesUtils.xor(f0, f1);
            // z1 = (e ☉ b1) ⊕ (f ☉ a1) ⊕ c1 ⊕ (e ☉ f)
            byte[] ef = BytesUtils.and(z1, f);
            BytesUtils.andi(z1, b1);
            BytesUtils.andi(f, a1);
            BytesUtils.xori(z1, f);
            BytesUtils.xori(z1, c1);
            BytesUtils.xori(z1, ef);
            SquareZ2Vector z1SquareVector = SquareZ2Vector.create(bitNum, z1, false);
            stopWatch.stop();
            long z1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, z1Time, "and (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "and");
            return z1SquareVector;
        }
    }
}
