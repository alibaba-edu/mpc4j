package edu.alibaba.mpc4j.s2pc.aby.basics.bc.rrg21;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.AbstractBcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.rrg21.Rrg21BcPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * RRG+21 Boolean circuit receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class Rrg21BcReceiver extends AbstractBcParty {
    /**
     * COT receiver
     */
    private final CotReceiver cotReceiver;
    /**
     * COT sender
     */
    private final CotSender cotSender;
    /**
     * -t1 vector
     */
    private BitVector negT1BitVector;
    /**
     * s1 vector
     */
    private BitVector s1BitVector;

    public Rrg21BcReceiver(Rpc receiverRpc, Party senderParty, Rrg21BcConfig config) {
        super(Rrg21BcPtoDesc.getInstance(), receiverRpc, senderParty, config);
        CotConfig cotConfig = config.getCotConfig();
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, cotConfig);
        addSubPtos(cotReceiver);
        cotSender = CotFactory.createSender(receiverRpc, senderParty, cotConfig);
        addSubPtos(cotSender);
    }

    @Override
    public void init(int maxRoundBitNum, int updateBitNum) throws MpcAbortException {
        setInitInput(maxRoundBitNum, updateBitNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // since storing many COT outputs would lead to memory exception, here we generate COT when necessary
        cotReceiver.init(maxRoundBitNum, updateBitNum);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, maxRoundBitNum, updateBitNum);
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
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_INPUT_SHARE.ordinal(), inputBitNum,
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
            // P1 invokes an instance of COT, where P1 is the receiver with inputs x1.
            byte[] x1Bytes = x1.getBitVector().getBytes();
            boolean[] x1Binary = BinaryUtils.byteArrayToBinary(x1Bytes, bitNum);
            CotReceiverOutput cotReceiverOutput = cotReceiver.receive(x1Binary);
            RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfFactory.CrhfType.MMO, cotReceiverOutput);
            // P1 invokes an instance of COT, where P1 is the sender.
            CotSenderOutput cotSenderOutput = cotSender.send(bitNum);
            RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfFactory.CrhfType.MMO, cotSenderOutput);
            stopWatch.stop();
            long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 4, cotTime);

            stopWatch.start();
            List<byte[]> delta1Payload = generateDelta1(rotSenderOutput, squareY1);
            DataPacketHeader delta1Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_DELTA1.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(delta1Header, delta1Payload));
            stopWatch.stop();
            long delta1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 4, delta1Time);

            DataPacketHeader delta0Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_DELTA0.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> delta0Payload = rpc.receive(delta0Header).getPayload();

            stopWatch.start();
            handleDelta0Payload(rotReceiverOutput, delta0Payload);
            stopWatch.stop();
            long delta0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 4, delta0Time);

            stopWatch.start();
            SquareZ2Vector z1 = generateZ1(squareX1, squareY1);
            negT1BitVector = null;
            s1BitVector = null;
            stopWatch.stop();
            long z1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 4, 4, z1Time);

            logPhaseInfo(PtoState.PTO_END, "and");
            return z1;
        }
    }

    private List<byte[]> generateDelta1(RotSenderOutput rotSenderOutput, SquareZ2Vector y1) {
        BitVector y = y1.getBitVector();
        BitVector t0s = BitVectorFactory.createZeros(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, bitNum);
        BitVector t1s = BitVectorFactory.createZeros(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, bitNum);
        // compute Δr, note that we cannot parallel execute the protocol
        IntStream.range(0, bitNum).forEach(index -> {
            t0s.set(index, rotSenderOutput.getR0(index)[0] % 2 == 1);
            t1s.set(index, rotSenderOutput.getR1(index)[0] % 2 == 1);
        });
        // Δ0 = Δ - Δr = Δ ⊕ Δr, where Δ = y1 − 2 * x1 * y1 = y1 ⊕ (0 ☉ x1 ☉ y1) = y1, hence Δ0 = y1 ⊕ Δr
        BitVector delta1Vector = t0s.xor(t1s);
        delta1Vector.xori(y);
        List<byte[]> delta1Payload = new LinkedList<>();
        delta1Payload.add(delta1Vector.getBytes());
        negT1BitVector = t0s.not();
        return delta1Payload;
    }

    private void handleDelta0Payload(RotReceiverOutput rotReceiverOutput, List<byte[]> delta0Payload)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(delta0Payload.size() == 1);
        BitVector delta0Vector = BitVectorFactory.create(
            BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, bitNum, delta0Payload.remove(0)
        );
        s1BitVector = BitVectorFactory.createZeros(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, bitNum);
        IntStream.range(0, bitNum).forEach(index -> {
            boolean x1 = rotReceiverOutput.getChoice(index);
            boolean t1 = rotReceiverOutput.getRb(index)[0] % 2 == 1;
            if (!x1) {
                s1BitVector.set(index, t1);
            } else {
                s1BitVector.set(index, t1 ^ delta0Vector.get(index));
            }
        });
    }

    private SquareZ2Vector generateZ1(SquareZ2Vector x1, SquareZ2Vector y1) {
        BitVector x = x1.getBitVector();
        BitVector y = y1.getBitVector();
        BitVector z1BitVector = BitVectorFactory.createZeros(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, bitNum);
        // x1 * y1 = x1 ☉ y1
        z1BitVector.xori(x);
        z1BitVector.andi(y);
        // x1 * y1 + x0 * (y1 − 2 * x1 * y1)
        z1BitVector.xori(negT1BitVector);
        // x1 * y1 + x0 * (y1 − 2 * x1 * y1) + x1 * (y0 − 2 * x0 * y0)
        z1BitVector.xori(s1BitVector);

        return SquareZ2Vector.create(z1BitVector, false);
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
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_OUTPUT_SHARE.ordinal(), outputBitNum,
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
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_SHARE_OUTPUT.ordinal(), outputBitNum,
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
