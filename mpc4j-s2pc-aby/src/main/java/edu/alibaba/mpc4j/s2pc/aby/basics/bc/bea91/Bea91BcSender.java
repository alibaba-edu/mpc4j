package edu.alibaba.mpc4j.s2pc.aby.basics.bc.bea91;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.AbstractBcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Bea91 Boolean circuit sender.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class Bea91BcSender extends AbstractBcParty {
    /**
     * Boolean triple generation sender
     */
    private final Z2MtgParty z2MtgSender;

    public Bea91BcSender(Rpc senderRpc, Party receiverParty, Bea91BcConfig config) {
        super(Bea91BcPtoDesc.getInstance(), senderRpc, receiverParty, config);
        z2MtgSender = Z2MtgFactory.createSender(senderRpc, receiverParty, config.getZ2MtgConfig());
        addSubPtos(z2MtgSender);
    }

    @Override
    public void init(int maxRoundBitNum, int updateBitNum) throws MpcAbortException {
        setInitInput(maxRoundBitNum, updateBitNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        z2MtgSender.init(maxRoundBitNum, updateBitNum);
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
        BitVector x0BitVector = BitVectorFactory.createRandom(bitNum, secureRandom);
        BitVector x1BitVector = x.xor(x0BitVector);
        List<byte[]> x1Payload = Collections.singletonList(x1BitVector.getBytes());
        DataPacketHeader x1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.SENDER_SEND_INPUT_SHARE.ordinal(), inputBitNum,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(x1Header, x1Payload));
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, shareTime, "send share");

        logPhaseInfo(PtoState.PTO_END, "send share");
        return SquareZ2Vector.create(x0BitVector, false);
    }

    @Override
    public SquareZ2Vector shareOther(int bitNum) throws MpcAbortException {
        setShareOtherInput(bitNum);
        logPhaseInfo(PtoState.PTO_BEGIN, "receive share");

        stopWatch.start();
        DataPacketHeader x0Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.RECEIVER_SEND_INPUT_SHARE.ordinal(), inputBitNum,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> x0Payload = rpc.receive(x0Header).getPayload();
        MpcAbortPreconditions.checkArgument(x0Payload.size() == 1);
        BitVector x0BitVector = BitVectorFactory.create(bitNum, x0Payload.get(0));
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, shareTime, "receive share");

        logPhaseInfo(PtoState.PTO_END, "receive share");
        return SquareZ2Vector.create(x0BitVector, false);
    }

    @Override
    public SquareZ2Vector and(MpcZ2Vector x0, MpcZ2Vector y0) throws MpcAbortException {
        SquareZ2Vector squareX0 = (SquareZ2Vector) x0;
        SquareZ2Vector squareY0 = (SquareZ2Vector) y0;
        setAndInput(squareX0, squareY0);
        if (x0.isPlain() && y0.isPlain()) {
            // x0 and y0 are plain bit vector, using plain AND.
            BitVector z0BitVector = x0.getBitVector().and(y0.getBitVector());
            return SquareZ2Vector.create(z0BitVector, true);
        } else if (x0.isPlain() || y0.isPlain()) {
            // x0 or y0 is plain bit vector, using plain AND.
            BitVector z0BitVector = x0.getBitVector().and(y0.getBitVector());
            return SquareZ2Vector.create(z0BitVector, false);
        } else {
            // x0 and y0 are secret bit vector, using secret AND.
            andGateNum += bitNum;
            logPhaseInfo(PtoState.PTO_BEGIN, "and");

            stopWatch.start();
            Z2Triple z2Triple = z2MtgSender.generate(bitNum);
            stopWatch.stop();
            long z2MtgTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 3, z2MtgTime, "and (gen. Boolean triples)");

            // compute e0 and f0
            stopWatch.start();
            byte[] a0 = z2Triple.getA();
            byte[] b0 = z2Triple.getB();
            byte[] c0 = z2Triple.getC();
            // e0 = x0 ⊕ a0
            byte[] e0 = BytesUtils.xor(x0.getBitVector().getBytes(), a0);
            // f0 = y0 ⊕ b0
            byte[] f0 = BytesUtils.xor(y0.getBitVector().getBytes(), b0);
            List<byte[]> e0f0Payload = new LinkedList<>();
            e0f0Payload.add(e0);
            e0f0Payload.add(f0);
            DataPacketHeader e0f0Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.SENDER_SEND_E0_F0.ordinal(), andGateNum,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(e0f0Header, e0f0Payload));
            stopWatch.stop();
            long e0f0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, e0f0Time, "and (open e/f)");

            stopWatch.start();
            DataPacketHeader e1f1Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.RECEIVER_SEND_E1_F1.ordinal(), andGateNum,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> e1f1Payload = rpc.receive(e1f1Header).getPayload();
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
            SquareZ2Vector squareZ0 = SquareZ2Vector.create(bitNum, z0, false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, z0Time, "and (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "and");
            return squareZ0;
        }
    }

    @Override
    public SquareZ2Vector xor(MpcZ2Vector x0, MpcZ2Vector y0) {
        SquareZ2Vector squareX0 = (SquareZ2Vector) x0;
        SquareZ2Vector squareY0 = (SquareZ2Vector) y0;
        setXorInput(squareX0, squareY0);
        if (x0.isPlain() && y0.isPlain()) {
            // x0 and y0 are plain bit vector, using plain XOR.
            BitVector z0BitVector = x0.getBitVector().xor(y0.getBitVector());
            return SquareZ2Vector.create(z0BitVector, true);
        } else if (x0.isPlain() || y0.isPlain()) {
            // x0 or y0 is plain bit vector, the sender does plain XOR.
            BitVector z0BitVector = x0.getBitVector().xor(y0.getBitVector());
            return SquareZ2Vector.create(z0BitVector, false);
        } else {
            // x0 and y0 are secret bit vector, using secret XOR.
            xorGateNum += bitNum;
            logPhaseInfo(PtoState.PTO_BEGIN, "xor");

            stopWatch.start();
            BitVector z0BitVector = x0.getBitVector().xor(y0.getBitVector());
            SquareZ2Vector squareZ0 = SquareZ2Vector.create(z0BitVector, false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, z0Time, "xor (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "xor");
            return squareZ0;
        }
    }

    @Override
    public BitVector revealOwn(SquareZ2Vector x0) throws MpcAbortException {
        setRevealOwnInput(x0);
        if (x0.isPlain()) {
            return x0.getBitVector();
        } else {
            outputBitNum += bitNum;
            logPhaseInfo(PtoState.PTO_BEGIN, "receive share");

            stopWatch.start();
            DataPacketHeader x1Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.RECEIVER_SEND_SHARE_OUTPUT.ordinal(), outputBitNum,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> x1Payload = rpc.receive(x1Header).getPayload();
            MpcAbortPreconditions.checkArgument(x1Payload.size() == 1);
            BitVector x0BitVector = x0.getBitVector();
            BitVector x1BitVector = BitVectorFactory.create(bitNum, x1Payload.get(0));
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "receive share");

            logPhaseInfo(PtoState.PTO_END, "receive share");
            return x0BitVector.xor(x1BitVector);
        }
    }

    @Override
    public void revealOther(SquareZ2Vector x0) {
        setRevealOtherInput(x0);
        if (!x0.isPlain()) {
            outputBitNum += bitNum;
            logPhaseInfo(PtoState.PTO_BEGIN, "send share");

            stopWatch.start();
            List<byte[]> x0Payload = Collections.singletonList(x0.getBitVector().getBytes());
            DataPacketHeader x0Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.SENDER_SEND_OUTPUT_SHARE.ordinal(), outputBitNum,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(x0Header, x0Payload));
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "send share");

            logPhaseInfo(PtoState.PTO_END, "send share");
        }
    }
}
