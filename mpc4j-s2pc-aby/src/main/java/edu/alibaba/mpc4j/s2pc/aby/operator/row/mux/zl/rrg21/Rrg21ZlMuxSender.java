package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.rrg21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.AbstractZlMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.rrg21.Rrg21ZlMuxPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RRG+21 Zl mux sender.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public class Rrg21ZlMuxSender extends AbstractZlMuxParty {
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
    private ZlVector negS0ZlVector;
    /**
     * t0 vector
     */
    private ZlVector t0ZlVector;

    public Rrg21ZlMuxSender(Rpc senderRpc, Party receiverParty, Rrg21ZlMuxConfig config) {
        super(Rrg21ZlMuxPtoDesc.getInstance(), senderRpc, receiverParty, config);
        cotSender = CotFactory.createSender(senderRpc, receiverParty, config.getCotConfig());
        addSubPto(cotSender);
        cotReceiver = CotFactory.createReceiver(senderRpc, receiverParty, config.getCotConfig());
        addSubPto(cotReceiver);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, maxNum);
        cotReceiver.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector mux(SquareZ2Vector x0, SquareZlVector y0) throws MpcAbortException {
        setPtoInput(x0, y0);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // P0 invokes an instance of COT, where P0 is the sender.
        CotSenderOutput cotSenderOutput = cotSender.send(num);
        // P0 invokes an instance of COT, where P0 is the receiver with inputs x0.
        byte[] x0Bytes = x0.getBitVector().getBytes();
        boolean[] x0Binary = BinaryUtils.byteArrayToBinary(x0Bytes, num);
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(x0Binary);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, cotTime);

        stopWatch.start();
        List<byte[]> delta0Payload = generateDelta0(cotSenderOutput, x0, y0);
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
        handleDelta1Payload(cotReceiverOutput, delta1Payload);
        stopWatch.stop();
        long delta1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, delta1Time);

        stopWatch.start();
        SquareZlVector z0 = generateZ0(x0, y0);
        negS0ZlVector = null;
        t0ZlVector = null;
        stopWatch.stop();
        long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, z0Time);

        logPhaseInfo(PtoState.PTO_END);
        return z0;
    }

    private List<byte[]> generateDelta0(CotSenderOutput cotSenderOutput, SquareZ2Vector x0, SquareZlVector y0) {
        BitVector x = x0.getBitVector();
        ZlVector y = y0.getZlVector();
        BigInteger[] s0s = new BigInteger[num];
        IntStream delta0IntStream = IntStream.range(0, num);
        delta0IntStream = parallel ? delta0IntStream.parallel() : delta0IntStream;
        List<byte[]> delta0Payload = delta0IntStream
            .mapToObj(index -> {
                s0s[index] = zl.createRandom(cotSenderOutput.getR0(index));
                BigInteger r = zl.createRandom(cotSenderOutput.getR1(index));
                // Δr
                BigInteger randomDelta = zl.sub(r, s0s[index]);
                // Δ = y0 − 2 * x0 * y0
                BigInteger targetDelta;
                if (x.get(index)) {
                    targetDelta = zl.mul(y.getElement(index), zl.module(BigIntegerUtils.BIGINT_2));
                    targetDelta = zl.neg(targetDelta);
                    targetDelta = zl.add(targetDelta, y.getElement(index));
                } else {
                    targetDelta = y.getElement(index);
                }
                // Δ0 = Δ - Δr
                BigInteger delta0 = zl.sub(targetDelta, randomDelta);
                return BigIntegerUtils.nonNegBigIntegerToByteArray(delta0, byteL);
            })
            .collect(Collectors.toList());
        ZlVector s0Vector = ZlVector.create(zl, s0s);
        negS0ZlVector = s0Vector.neg();
        return delta0Payload;
    }

    private void handleDelta1Payload(CotReceiverOutput cotReceiverOutput, List<byte[]> delta1Payload)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(delta1Payload.size() == num);
        BigInteger[] delta1s = delta1Payload.stream()
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
        IntStream delta1IntStream = IntStream.range(0, num);
        delta1IntStream = parallel ? delta1IntStream.parallel() : delta1IntStream;
        BigInteger[] t0s = delta1IntStream
            .mapToObj(index -> {
                boolean x0 = cotReceiverOutput.getChoice(index);
                BigInteger t0 = zl.createRandom(cotReceiverOutput.getRb(index));
                if (!x0) {
                    return t0;
                } else {
                    return zl.add(t0, delta1s[index]);
                }
            })
            .toArray(BigInteger[]::new);
        t0ZlVector = ZlVector.create(zl, t0s);
    }

    private SquareZlVector generateZ0(SquareZ2Vector x0, SquareZlVector y0) {
        BitVector x = x0.getBitVector();
        ZlVector y = y0.getZlVector();
        IntStream z0IntStream = IntStream.range(0, num);
        z0IntStream = parallel ? z0IntStream.parallel() : z0IntStream;
        BigInteger[] z0s = z0IntStream
            .mapToObj(index -> {
                // x0 * y0
                BigInteger z0 = x.get(index) ? y.getElement(index) : zl.createZero();
                // x0 * y0 + x1 * (y0 − 2 * x0 * y0)
                z0 = zl.add(z0, negS0ZlVector.getElement(index));
                // x0 * y0 + x0 * (y1 − 2 * x1 * y1)
                z0 = zl.add(z0, t0ZlVector.getElement(index));
                return z0;
            })
            .toArray(BigInteger[]::new);
        ZlVector z0ZlVector = ZlVector.create(zl, z0s);
        return SquareZlVector.create(z0ZlVector, false);
    }
}
