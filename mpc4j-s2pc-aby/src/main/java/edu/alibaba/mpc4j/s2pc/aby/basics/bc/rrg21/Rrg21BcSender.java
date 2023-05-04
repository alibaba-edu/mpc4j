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
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.rrg21.Rrg21BcPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * RRG+21 Boolean circuit sender.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class Rrg21BcSender extends AbstractBcParty {
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
    private BitVector negS0BitVector;
    /**
     * t0 vector
     */
    private BitVector t0BitVector;

    public Rrg21BcSender(Rpc senderRpc, Party receiverParty, Rrg21BcConfig config) {
        super(Rrg21BcPtoDesc.getInstance(), senderRpc, receiverParty, config);
        CotConfig cotConfig = config.getCotConfig();
        cotSender = CotFactory.createSender(senderRpc, receiverParty, cotConfig);
        addSubPtos(cotSender);
        cotReceiver = CotFactory.createReceiver(senderRpc, receiverParty, cotConfig);
        addSubPtos(cotReceiver);
    }

    @Override
    public void init(int maxRoundBitNum, int updateBitNum) throws MpcAbortException {
        setInitInput(maxRoundBitNum, updateBitNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        // since storing many COT outputs would lead to memory exception, here we generate COT when necessary
        cotSender.init(delta, maxRoundBitNum, updateBitNum);
        cotReceiver.init(maxRoundBitNum, updateBitNum);
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
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_INPUT_SHARE.ordinal(), inputBitNum,
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
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_INPUT_SHARE.ordinal(), inputBitNum,
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
            List<byte[]> delta0Payload = generateDelta0(rotSenderOutput, squareY0);
            DataPacketHeader delta0Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_DELTA0.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(delta0Header, delta0Payload));
            stopWatch.stop();
            long delta0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 4, delta0Time);

            DataPacketHeader delta1Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_DELTA1.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> delta1Payload = rpc.receive(delta1Header).getPayload();

            stopWatch.start();
            handleDelta1Payload(rotReceiverOutput, delta1Payload);
            stopWatch.stop();
            long delta1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 4, delta1Time);

            stopWatch.start();
            SquareZ2Vector z0 = generateZ0(squareX0, squareY0);
            negS0BitVector = null;
            t0BitVector = null;
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 4, 4, z0Time);

            logPhaseInfo(PtoState.PTO_END, "and");
            return z0;
        }
    }

    private List<byte[]> generateDelta0(RotSenderOutput rotSenderOutput, SquareZ2Vector y0) {
        BitVector y = y0.getBitVector();
        BitVector s0s = BitVectorFactory.createZeros(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, bitNum);
        BitVector s1s = BitVectorFactory.createZeros(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, bitNum);
        // compute Δr, note that we cannot parallel execute the protocol
        IntStream.range(0, bitNum).forEach(index -> {
            s0s.set(index, rotSenderOutput.getR0(index)[0] % 2 == 1);
            s1s.set(index, rotSenderOutput.getR1(index)[0] % 2 == 1);
        });
        // Δ0 = Δ - Δr = Δ ⊕ Δr, where Δ = y0 − 2 * x0 * y0 = y0 ⊕ (0 ☉ x0 ☉ y0) = y0, hence Δ0 = y0 ⊕ Δr
        BitVector delta0Vector = s0s.xor(s1s);
        delta0Vector.xori(y);
        List<byte[]> delta0Payload = new LinkedList<>();
        delta0Payload.add(delta0Vector.getBytes());
        negS0BitVector = s0s.not();
        return delta0Payload;
    }

    private void handleDelta1Payload(RotReceiverOutput rotReceiverOutput, List<byte[]> delta1Payload)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(delta1Payload.size() == 1);
        BitVector delta1Vector = BitVectorFactory.create(
            BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, bitNum, delta1Payload.remove(0)
        );
        t0BitVector = BitVectorFactory.createZeros(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, bitNum);
        IntStream.range(0, bitNum).forEach(index -> {
            boolean x0 = rotReceiverOutput.getChoice(index);
            boolean t0 = rotReceiverOutput.getRb(index)[0] % 2 == 1;
            if (!x0) {
                t0BitVector.set(index, t0);
            } else {
                t0BitVector.set(index, t0 ^ delta1Vector.get(index));
            }
        });
    }

    private SquareZ2Vector generateZ0(SquareZ2Vector x0, SquareZ2Vector y0) {
        BitVector x = x0.getBitVector();
        BitVector y = y0.getBitVector();
        BitVector z0BitVector = BitVectorFactory.createZeros(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, bitNum);
        // x0 * y0 = x0 ☉ y0
        z0BitVector.xori(x);
        z0BitVector.andi(y);
        // x0 * y0 + x1 * (y0 − 2 * x0 * y0)
        z0BitVector.xori(negS0BitVector);
        // x0 * y0 + x0 * (y1 − 2 * x1 * y1)
        z0BitVector.xori(t0BitVector);

        return SquareZ2Vector.create(z0BitVector, false);
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
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_SHARE_OUTPUT.ordinal(), outputBitNum,
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
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_OUTPUT_SHARE.ordinal(), outputBitNum,
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
