//package edu.alibaba.mpc4j.s2pc.aby.operator.row.matrixMul.zl.rrgg21;
//
//import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
//import edu.alibaba.mpc4j.common.rpc.Party;
//import edu.alibaba.mpc4j.common.rpc.PtoState;
//import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
//import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
//import edu.alibaba.mpc4j.common.tool.CommonConstants;
//import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
//import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
//import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
//import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
//import edu.alibaba.mpc4j.s2pc.aby.operator.row.matrixMul.zl.AbstractZlCrossTermReceiver;
//import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
//import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
//import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
//
//import java.math.BigInteger;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
//import static edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.rrgg21.Rrgg21ZlCrossTermPtoDesc.PtoStep;
//import static edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.rrgg21.Rrgg21ZlCrossTermPtoDesc.getInstance;
//
///**
// * RRGG21 Zl Cross Term Multiplication Receiver.
// *
// * @author Liqiang Peng
// * @date 2024/6/5
// */
//public class Rrgg21ZlCrossTermReceiver extends AbstractZlCrossTermReceiver {
//    /**
//     * cot sender
//     */
//    private final CotSender cotSender;
//
//    public Rrgg21ZlCrossTermReceiver(Z2cParty z2cReceiver, Party senderParty, Rrgg21ZlCrossTermConfig config) {
//        super(getInstance(), z2cReceiver.getRpc(), senderParty, config);
//        cotSender = CotFactory.createSender(z2cReceiver.getRpc(), senderParty, config.getCotConfig());
//        addSubPto(cotSender);
//    }
//
//    @Override
//    public void init(int maxInputL, int maxOutputL, int maxNum) throws MpcAbortException {
//        setInitInput(maxInputL, maxOutputL, maxNum);
//        logPhaseInfo(PtoState.INIT_BEGIN);
//
//        stopWatch.start();
//        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
//        cotSender.init(delta, maxNum);
//        stopWatch.stop();
//        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);
//
//        logPhaseInfo(PtoState.INIT_END);
//    }
//
//    @Override
//    public SquareZlVector crossTerm(SquareZlVector y, int m) throws MpcAbortException {
//        setPtoInput(y, m);
//        logPhaseInfo(PtoState.PTO_BEGIN);
//
//        stopWatch.start();
//        BigInteger[] result = IntStream.range(0, num).mapToObj(i -> BigInteger.ZERO).toArray(BigInteger[]::new);
//        for (int i = 0; i < m; i++) {
//            CotSenderOutput cotSenderOutput = cotSender.send(num);
//            List<byte[]> encPayload;
//            IntStream intStream = IntStream.range(0, num);
//            intStream = parallel ? intStream.parallel() : intStream;
//            int finalI = i;
//            encPayload = intStream.mapToObj(j -> {
//                BigInteger input = BigIntegerUtils.byteArrayToBigInteger(ys[j]);
//                BigInteger t = BigIntegerUtils.byteArrayToBigInteger(cotSenderOutput.getR0(j));
//                BigInteger message = BigIntegerUtils.byteArrayToBigInteger(cotSenderOutput.getR1(j))
//                    .subtract(input.shiftLeft(finalI).subtract(t))
//                    .mod(outputZl.getRangeBound());
//                result[j] = result[j].add(t).mod(outputZl.getRangeBound());
//                return BigIntegerUtils.bigIntegerToByteArray(message);
//            }).collect(Collectors.toList());
//            DataPacketHeader encPayloadHeader = new DataPacketHeader(
//                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_ENC_ELEMENTS.ordinal(), extraInfo++,
//                ownParty().getPartyId(), otherParty().getPartyId()
//            );
//            rpc.send(DataPacket.fromByteArrayList(encPayloadHeader, encPayload));
//        }
//        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//        logStepInfo(PtoState.PTO_STEP, 1, 1, ptoTime);
//
//        logPhaseInfo(PtoState.PTO_END);
//        return SquareZlVector.create(outputZl, result, false);
//    }
//}
