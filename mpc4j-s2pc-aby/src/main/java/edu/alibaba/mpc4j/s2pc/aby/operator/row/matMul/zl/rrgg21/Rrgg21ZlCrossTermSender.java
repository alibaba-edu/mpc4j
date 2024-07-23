//package edu.alibaba.mpc4j.s2pc.aby.operator.row.matrixMul.zl.rrgg21;
//
//import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
//import edu.alibaba.mpc4j.common.rpc.Party;
//import edu.alibaba.mpc4j.common.rpc.PtoState;
//import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
//import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
//import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
//import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
//import edu.alibaba.mpc4j.s2pc.aby.operator.row.matrixMul.zl.AbstractZlCrossTermSender;
//import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
//import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
//import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
//
//import java.math.BigInteger;
//import java.util.ArrayList;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.IntStream;
//
//import static edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.rrgg21.Rrgg21ZlCrossTermPtoDesc.PtoStep;
//import static edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.rrgg21.Rrgg21ZlCrossTermPtoDesc.getInstance;
//
///**
// * RRGG21 Zl Cross Term Multiplication Sender.
// *
// * @author Liqiang Peng
// * @date 2024/6/5
// */
//public class Rrgg21ZlCrossTermSender extends AbstractZlCrossTermSender {
//    /**
//     * cot receiver
//     */
//    private final CotReceiver cotReceiver;
//
//    public Rrgg21ZlCrossTermSender(Z2cParty z2cSender, Party receiverParty, Rrgg21ZlCrossTermConfig config) {
//        super(getInstance(), z2cSender.getRpc(), receiverParty, config);
//        cotReceiver = CotFactory.createReceiver(z2cSender.getRpc(), receiverParty, config.getCotConfig());
//        addSubPto(cotReceiver);
//    }
//
//    @Override
//    public void init(int maxM, int maxN, int maxNum) throws MpcAbortException {
//        setInitInput(maxM, maxN, maxNum);
//        logPhaseInfo(PtoState.INIT_BEGIN);
//
//        stopWatch.start();
//        cotReceiver.init(maxNum);
//        stopWatch.stop();
//        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);
//
//        logPhaseInfo(PtoState.INIT_END);
//    }
//
//    @Override
//    public SquareZlVector crossTerm(SquareZlVector x, int n) throws MpcAbortException {
//        setPtoInput(x, n);
//        logPhaseInfo(PtoState.PTO_BEGIN);
//
//        stopWatch.start();
//        BigInteger[] result = IntStream.range(0, num).mapToObj(i -> BigInteger.ZERO).toArray(BigInteger[]::new);
//        for (int i = 0; i < m; i++) {
//            boolean[] choices = new boolean[num];
//            for (int j = 0; j < num; j++) {
//                choices[j] = xs[j][m - i - 1];
//            }
//            CotReceiverOutput cotReceiverOutput = cotReceiver.receive(choices);
//            DataPacketHeader encPayloadHeader = new DataPacketHeader(
//                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_ENC_ELEMENTS.ordinal(), extraInfo++,
//                otherParty().getPartyId(), ownParty().getPartyId()
//            );
//            ArrayList<byte[]> encPayload = (ArrayList<byte[]>) rpc.receive(encPayloadHeader).getPayload();
//            IntStream intStream = IntStream.range(0, num);
//            intStream = parallel ? intStream.parallel() : intStream;
//            intStream.forEach(j -> {
//                BigInteger t;
//                if (cotReceiverOutput.getChoice(j)) {
//                    BigInteger rPlusDelta = BigIntegerUtils.byteArrayToBigInteger(cotReceiverOutput.getRb(j));
//                    t = rPlusDelta.subtract(BigIntegerUtils.byteArrayToBigInteger(encPayload.get(j)));
//                } else {
//                    t = BigIntegerUtils.byteArrayToBigInteger(cotReceiverOutput.getRb(j)).negate();
//                }
//                result[j] = result[j].add(t).mod(outputZl.getRangeBound());
//            });
//        }
//        stopWatch.stop();
//        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//        logStepInfo(PtoState.PTO_STEP, 1, 1, ptoTime);
//
//        logPhaseInfo(PtoState.PTO_END);
//        return SquareZlVector.create(outputZl, result, false);
//    }
//}