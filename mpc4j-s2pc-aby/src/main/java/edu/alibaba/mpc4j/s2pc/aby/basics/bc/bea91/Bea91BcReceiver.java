package edu.alibaba.mpc4j.s2pc.aby.basics.bc.bea91;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.AbstractBcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.bea91.Bea91BcPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Bea91 Boolean circuit receiver.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class Bea91BcReceiver extends AbstractBcParty {
    /**
     * Boolean triple generation receiver
     */
    private final Z2MtgParty z2MtgReceiver;

    public Bea91BcReceiver(Rpc receiverRpc, Party senderParty, Bea91BcConfig config) {
        super(Bea91BcPtoDesc.getInstance(), receiverRpc, senderParty, config);
        z2MtgReceiver = Z2MtgFactory.createReceiver(receiverRpc, senderParty, config.getZ2MtgConfig());
        addSubPtos(z2MtgReceiver);
    }

    @Override
    public void init(int maxRoundBitNum, int updateBitNum) throws MpcAbortException {
        setInitInput(maxRoundBitNum, updateBitNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        z2MtgReceiver.init(maxRoundBitNum, updateBitNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector shareOwn(BitVector x) {
        setShareOwnInput(x);
        logPhaseInfo(PtoState.PTO_BEGIN, "send share");

        stopWatch.start();
        BitVector x1BitVector = BitVectorFactory.createRandom(bitNum, secureRandom);
        BitVector x0BitVector = x.xor(x1BitVector);
        List<byte[]> x0Payload = Collections.singletonList(x0BitVector.getBytes());
        DataPacketHeader x0Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.RECEIVER_SEND_INPUT_SHARE.ordinal(), inputBitNum,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(x0Header, x0Payload));
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, shareTime, "send share");

        logPhaseInfo(PtoState.PTO_END, "send share");
        return SquareZ2Vector.create(x1BitVector, false);
    }

    @Override
    public SquareZ2Vector shareOther(int bitNum) throws MpcAbortException {
        setShareOtherInput(bitNum);
        logPhaseInfo(PtoState.PTO_BEGIN, "receive share");

        stopWatch.start();
        DataPacketHeader x1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_INPUT_SHARE.ordinal(), inputBitNum,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> x1Payload = rpc.receive(x1Header).getPayload();
        MpcAbortPreconditions.checkArgument(x1Payload.size() == 1);
        BitVector x1BitVector = BitVectorFactory.create(bitNum, x1Payload.get(0));
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, shareTime, "receive share");

        logPhaseInfo(PtoState.PTO_END, "receive share");
        return SquareZ2Vector.create(x1BitVector, false);
    }

    @Override
    public SquareZ2Vector and(MpcZ2Vector x1, MpcZ2Vector y1) throws MpcAbortException {
        SquareZ2Vector squareX1 = (SquareZ2Vector) x1;
        SquareZ2Vector squareY1 = (SquareZ2Vector) y1;
        setAndInput(squareX1, squareY1);

        if (x1.isPlain() && y1.isPlain()) {
            // x1 and y1 are plain bit vectors, using plain AND.
            BitVector z1BitVector = x1.getBitVector().and(y1.getBitVector());
            return SquareZ2Vector.create(z1BitVector, true);
        } else if (x1.isPlain() || y1.isPlain()) {
            // x1 or y1 is plain bit vector, using plain AND.
            BitVector z1BitVector = x1.getBitVector().and(y1.getBitVector());
            return SquareZ2Vector.create(z1BitVector, false);
        } else {
            // x1 and y1 are secret bit vectors, using secret AND.
            andGateNum += bitNum;
            logPhaseInfo(PtoState.PTO_BEGIN, "and");

            stopWatch.start();
            Z2Triple z2Triple = z2MtgReceiver.generate(bitNum);
            stopWatch.stop();
            long z2MtgTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 3, z2MtgTime, "and (gen. Boolean triples)");

            stopWatch.start();
            byte[] a1 = z2Triple.getA();
            byte[] b1 = z2Triple.getB();
            byte[] c1 = z2Triple.getC();
            // e1 = x1 ⊕ a1
            byte[] e1 = BytesUtils.xor(x1.getBitVector().getBytes(), a1);
            // f1 = y1 ⊕ b1
            byte[] f1 = BytesUtils.xor(y1.getBitVector().getBytes(), b1);
            List<byte[]> e1f1Payload = new LinkedList<>();
            e1f1Payload.add(e1);
            e1f1Payload.add(f1);
            DataPacketHeader e1f1Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_E1_F1.ordinal(), andGateNum,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(e1f1Header, e1f1Payload));
            stopWatch.stop();
            long e1f1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, e1f1Time, "and (open e/f)");

            stopWatch.start();
            DataPacketHeader e0f0Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.SENDER_SEND_E0_F0.ordinal(), andGateNum,
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
            SquareZ2Vector squareZ1 = SquareZ2Vector.create(bitNum, z1, false);
            stopWatch.stop();
            long z1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, z1Time, "and (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "and");
            return squareZ1;
        }
    }

    @Override
    public SquareZ2Vector xor(MpcZ2Vector x1, MpcZ2Vector y1) {
        SquareZ2Vector squareX1 = (SquareZ2Vector) x1;
        SquareZ2Vector squareY1 = (SquareZ2Vector) y1;
        setXorInput(squareX1, squareY1);

        if (x1.isPlain() && y1.isPlain()) {
            // x1 and y1 are plain bit vector, using plain XOR.
            BitVector z1BitVector = x1.getBitVector().xor(y1.getBitVector());
            return SquareZ2Vector.create(z1BitVector, true);
        } else if (x1.isPlain()) {
            // x1 is plain bit vector, y1 is secret bit vector, the receiver copies y1
            return squareY1.copy();
        } else if (y1.isPlain()) {
            // x1 is secret bit vector, y1 is plain bit vector, the receiver copies x1
            return squareX1.copy();
        } else {
            // x1 and y1 are secret bit vectors, using secret XOR.
            xorGateNum += bitNum;
            logPhaseInfo(PtoState.PTO_BEGIN, "xor");

            stopWatch.start();
            BitVector z1BitVector = x1.getBitVector().xor(y1.getBitVector());
            SquareZ2Vector squareZ1 = SquareZ2Vector.create(z1BitVector, false);
            stopWatch.stop();
            long z1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, z1Time, "xor (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "xor");
            return squareZ1;
        }
    }

    @Override
    public BitVector revealOwn(SquareZ2Vector x1) throws MpcAbortException {
        setRevealOwnInput(x1);
        if (x1.isPlain()) {
            return x1.getBitVector();
        } else {
            outputBitNum += bitNum;
            logPhaseInfo(PtoState.PTO_BEGIN, "receive share");

            stopWatch.start();
            DataPacketHeader x0Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.SENDER_SEND_OUTPUT_SHARE.ordinal(), outputBitNum,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> x0Payload = rpc.receive(x0Header).getPayload();
            MpcAbortPreconditions.checkArgument(x0Payload.size() == 1);
            BitVector x0BitVector = BitVectorFactory.create(bitNum, x0Payload.get(0));
            BitVector x1BitVector = x1.getBitVector();
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "receive share");

            logPhaseInfo(PtoState.PTO_END, "receive share");
            return x0BitVector.xor(x1BitVector);
        }
    }

    @Override
    public void revealOther(SquareZ2Vector x1) {
        setRevealOtherInput(x1);
        if (!x1.isPlain()) {
            outputBitNum += bitNum;
            logPhaseInfo(PtoState.PTO_BEGIN, "send share");

            stopWatch.start();
            List<byte[]> x1Payload = Collections.singletonList(x1.getBitVector().getBytes());
            DataPacketHeader x1Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.RECEIVER_SEND_SHARE_OUTPUT.ordinal(), outputBitNum,
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
}
