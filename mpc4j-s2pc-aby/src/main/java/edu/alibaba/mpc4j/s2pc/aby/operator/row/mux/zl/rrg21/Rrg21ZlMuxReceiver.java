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
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RRG+21 Zl mux receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public class Rrg21ZlMuxReceiver extends AbstractZlMuxParty {
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
    private ZlVector negT1ZlVector;
    /**
     * s1 vector
     */
    private ZlVector s1ZlVector;

    public Rrg21ZlMuxReceiver(Rpc receiverRpc, Party senderParty, Rrg21ZlMuxConfig config) {
        super(Rrg21ZlMuxPtoDesc.getInstance(), receiverRpc, senderParty, config);
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        addSubPto(cotReceiver);
        cotSender = CotFactory.createSender(receiverRpc, senderParty, config.getCotConfig());
        addSubPto(cotSender);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        cotReceiver.init(maxNum);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector mux(SquareZ2Vector x1, SquareZlVector y1) throws MpcAbortException {
        setPtoInput(x1, y1);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // P1 invokes an instance of COT, where P1 is the receiver with inputs x1.
        byte[] x1Bytes = x1.getBitVector().getBytes();
        boolean[] x1Binary = BinaryUtils.byteArrayToBinary(x1Bytes, num);
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(x1Binary);
        // P1 invokes an instance of COT, where P1 is the sender.
        CotSenderOutput cotSenderOutput = cotSender.send(num);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, cotTime);

        stopWatch.start();
        List<byte[]> delta1Payload = generateDelta1(cotSenderOutput, x1, y1);
        DataPacketHeader delta1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rrg21ZlMuxPtoDesc.PtoStep.RECEIVER_SEND_DELTA1.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(delta1Header, delta1Payload));
        stopWatch.stop();
        long delta1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, delta1Time);

        DataPacketHeader delta0Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rrg21ZlMuxPtoDesc.PtoStep.SENDER_SEND_DELTA0.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> delta0Payload = rpc.receive(delta0Header).getPayload();

        stopWatch.start();
        handleDelta0Payload(cotReceiverOutput, delta0Payload);
        stopWatch.stop();
        long delta0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, delta0Time);

        stopWatch.start();
        SquareZlVector z1 = generateZ1(x1, y1);
        negT1ZlVector = null;
        s1ZlVector = null;
        stopWatch.stop();
        long z1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, z1Time);

        logPhaseInfo(PtoState.PTO_END);
        return z1;
    }

    private List<byte[]> generateDelta1(CotSenderOutput cotSenderOutput, SquareZ2Vector x1, SquareZlVector y1) {
        BitVector x = x1.getBitVector();
        ZlVector y = y1.getZlVector();
        BigInteger[] t1s = new BigInteger[num];
        IntStream delta1IntStream = IntStream.range(0, num);
        delta1IntStream = parallel ? delta1IntStream.parallel() : delta1IntStream;
        List<byte[]> delta1Payload = delta1IntStream
            .mapToObj(index -> {
                t1s[index] = zl.createRandom(cotSenderOutput.getR0(index));
                BigInteger r = zl.createRandom(cotSenderOutput.getR1(index));
                // Δr
                BigInteger randomDelta = zl.sub(r, t1s[index]);
                // Δ = y1 − 2 * x1 * y1
                BigInteger targetDelta;
                if (x.get(index)) {
                    targetDelta = zl.mul(y.getElement(index), zl.module(BigIntegerUtils.BIGINT_2));
                    targetDelta = zl.neg(targetDelta);
                    targetDelta = zl.add(targetDelta, y.getElement(index));
                } else {
                    targetDelta = y.getElement(index);
                }
                // Δ1 = Δ - Δr
                BigInteger delta1 = zl.sub(targetDelta, randomDelta);
                return BigIntegerUtils.nonNegBigIntegerToByteArray(delta1, byteL);
            })
            .collect(Collectors.toList());
        ZlVector t1Vector = ZlVector.create(zl, t1s);
        negT1ZlVector = t1Vector.neg();
        return delta1Payload;
    }

    private void handleDelta0Payload(CotReceiverOutput cotReceiverOutput, List<byte[]> delta0Payload)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(delta0Payload.size() == num);
        BigInteger[] delta0s = delta0Payload.stream()
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
        IntStream delta0IntStream = IntStream.range(0, num);
        delta0IntStream = parallel ? delta0IntStream.parallel() : delta0IntStream;
        BigInteger[] s1s = delta0IntStream
            .mapToObj(index -> {
                boolean x1 = cotReceiverOutput.getChoice(index);
                BigInteger s1 = zl.createRandom(cotReceiverOutput.getRb(index));
                if (!x1) {
                    return s1;
                } else {
                    return zl.add(s1, delta0s[index]);
                }
            })
            .toArray(BigInteger[]::new);
        s1ZlVector = ZlVector.create(zl, s1s);
    }

    private SquareZlVector generateZ1(SquareZ2Vector x1, SquareZlVector y1) {
        BitVector x = x1.getBitVector();
        ZlVector y = y1.getZlVector();
        IntStream z1IntStream = IntStream.range(0, num);
        z1IntStream = parallel ? z1IntStream.parallel() : z1IntStream;
        BigInteger[] z1s = z1IntStream
            .mapToObj(index -> {
                // x1 * y1
                BigInteger z1 = x.get(index) ? y.getElement(index) : zl.createZero();
                // x1 * y1 + x0 * (y1 − 2 * x1 * y1)
                z1 = zl.add(z1, negT1ZlVector.getElement(index));
                // x1 * y1 + x0 * (y1 − 2 * x1 * y1) + x1 * (y0 − 2 * x0 * y0)
                z1 = zl.add(z1, s1ZlVector.getElement(index));
                return z1;
            })
            .toArray(BigInteger[]::new);
        ZlVector z1ZlVector = ZlVector.create(zl, z1s);
        return SquareZlVector.create(z1ZlVector, false);
    }
}
