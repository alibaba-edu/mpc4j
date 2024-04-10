package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.AbstractZlMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.rrk20.Rrk20ZlMuxPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RRK+20 Zl mux receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public class Rrk20ZlMuxReceiver extends AbstractZlMuxParty {
    /**
     * COT receiver
     */
    private final CotReceiver cotReceiver;
    /**
     * COT sender
     */
    private final CotSender cotSender;
    /**
     * R1 vector
     */
    private ZlVector r1ZlVector;
    /**
     * t0
     */
    private byte[][] t0s;
    /**
     * t1
     */
    private byte[][] t1s;

    public Rrk20ZlMuxReceiver(Rpc receiverRpc, Party senderParty, Rrk20ZlMuxConfig config) {
        super(Rrk20ZlMuxPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
        prepare(x1, y1);
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, prepareTime);

        stopWatch.start();
        // P1 invokes an instance of COT, where P1 is the receiver with inputs x1.
        byte[] x1Bytes = x1.getBitVector().getBytes();
        boolean[] x1Binary = BinaryUtils.byteArrayToBinary(x1Bytes, num);
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(x1Binary);
        // P1 invokes an instance of COT, where P1 is the sender with inputs (t0, t1).
        CotSenderOutput cotSenderOutput = cotSender.send(num);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, cotTime);

        stopWatch.start();
        t0t1(cotSenderOutput);
        t0s = null;
        t1s = null;
        stopWatch.stop();
        long s0s1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, s0s1Time);

        DataPacketHeader s0s1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_S0_S1.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> s0s1Payload = rpc.receive(s0s1Header).getPayload();

        stopWatch.start();
        SquareZlVector z1 = s0s1(cotReceiverOutput, s0s1Payload);
        r1ZlVector = null;
        stopWatch.stop();
        long t0t1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, t0t1Time);

        logPhaseInfo(PtoState.PTO_END);
        return z1;
    }

    private void prepare(SquareZ2Vector x1, SquareZlVector y1) {
        // P1 picks r1 ∈ Zn
        r1ZlVector = ZlVector.createRandom(zl, num, secureRandom);
        ZlVector negR1ZlVector = r1ZlVector.neg();
        // if x1 = 0, P1 sets (t0, t1) = (-r1, -r1 + y1), else, P1 sets (t0, t1) = (-r1 + y1, -r1).
        BitVector x1BitVector = x1.getBitVector();
        ZlVector y1ZlVector = y1.getZlVector();
        t0s = new byte[num][];
        t1s = new byte[num][];
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(index -> {
            boolean x = x1BitVector.get(index);
            if (!x) {
                t0s[index] = BigIntegerUtils.nonNegBigIntegerToByteArray(
                    negR1ZlVector.getElement(index), byteL
                );
                t1s[index] = BigIntegerUtils.nonNegBigIntegerToByteArray(
                    zl.add(negR1ZlVector.getElement(index), y1ZlVector.getElement(index)), byteL
                );
            } else {
                t0s[index] = BigIntegerUtils.nonNegBigIntegerToByteArray(
                    zl.add(negR1ZlVector.getElement(index), y1ZlVector.getElement(index)), byteL
                );
                t1s[index] = BigIntegerUtils.nonNegBigIntegerToByteArray(
                    negR1ZlVector.getElement(index), byteL
                );
            }
        });
    }

    private void t0t1(CotSenderOutput cotSenderOutput) {
        Prg prg = PrgFactory.createInstance(envType, byteL);
        // P1 creates t0
        IntStream t0IntStream = IntStream.range(0, num);
        t0IntStream = parallel ? t0IntStream.parallel() : t0IntStream;
        List<byte[]> t0t1Payload = t0IntStream
            .mapToObj(index -> {
                byte[] t0 = prg.extendToBytes(cotSenderOutput.getR0(index));
                BytesUtils.xori(t0, t0s[index]);
                return t0;
            })
            .collect(Collectors.toList());
        // P1 creates t1
        IntStream t1IntStream = IntStream.range(0, num);
        t1IntStream = parallel ? t1IntStream.parallel() : t1IntStream;
        List<byte[]> t1Payload = t1IntStream
            .mapToObj(index -> {
                byte[] t1 = prg.extendToBytes(cotSenderOutput.getR1(index));
                BytesUtils.xori(t1, t1s[index]);
                return t1;
            })
            .collect(Collectors.toList());
        // merge t0 and t1
        t0t1Payload.addAll(t1Payload);
        // sends s0 and s1
        DataPacketHeader t0t1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_T0_T1.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(t0t1Header, t0t1Payload));
    }

    private SquareZlVector s0s1(CotReceiverOutput cotReceiverOutput, List<byte[]> s0s1Payload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(s0s1Payload.size() == num * 2);
        byte[][] s0s = s0s1Payload.subList(0, num).toArray(new byte[0][]);
        byte[][] s1s = s0s1Payload.subList(num, num * 2).toArray(new byte[0][]);
        Prg prg = PrgFactory.createInstance(envType, byteL);
        // Let P1's output be a1
        IntStream s0IntStream = IntStream.range(0, num);
        s0IntStream = parallel ? s0IntStream.parallel() : s0IntStream;
        BigInteger[] a1s = s0IntStream
            .mapToObj(index -> {
                boolean x1 = cotReceiverOutput.getChoice(index);
                byte[] a1 = prg.extendToBytes(cotReceiverOutput.getRb(index));
                if (!x1) {
                    BytesUtils.xori(a1, s0s[index]);
                } else {
                    BytesUtils.xori(a1, s1s[index]);
                }
                return a1;
            })
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
        ZlVector z1ZlVector = ZlVector.create(zl, a1s);
        z1ZlVector.addi(r1ZlVector);
        return SquareZlVector.create(z1ZlVector, false);
    }
}
