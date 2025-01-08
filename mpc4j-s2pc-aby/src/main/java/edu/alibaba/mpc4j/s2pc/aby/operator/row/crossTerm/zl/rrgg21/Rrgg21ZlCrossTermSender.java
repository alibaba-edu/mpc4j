package edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.rrgg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.AbstractZlCrossTermSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.rrgg21.Rrgg21ZlCrossTermPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.rrgg21.Rrgg21ZlCrossTermPtoDesc.getInstance;

/**
 * RRGG21 Zl Cross Term Multiplication Sender.
 *
 * @author Liqiang Peng
 * @date 2024/6/5
 */
public class Rrgg21ZlCrossTermSender extends AbstractZlCrossTermSender {
    /**
     * cot receiver
     */
    private final CotReceiver[] cotReceiver;

    public Rrgg21ZlCrossTermSender(Z2cParty z2cSender, Party receiverParty, Rrgg21ZlCrossTermConfig config) {
        super(getInstance(), z2cSender.getRpc(), receiverParty, config);
        cotReceiver = new CotReceiver[Long.SIZE];
        IntStream.range(0, Long.SIZE).forEach(i -> {
            cotReceiver[i] = CotFactory.createReceiver(z2cSender.getRpc(), receiverParty, config.getCotConfig());
            addSubPto(cotReceiver[i]);
        });
    }

    @Override
    public void init(int maxM, int maxN) throws MpcAbortException {
        setInitInput(maxM, maxN);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        for (int i = 0; i < Long.SIZE; i++) {
            cotReceiver[i].init(1);
        }
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BigInteger crossTerm(BigInteger x, int m, int n) throws MpcAbortException {
        setPtoInput(x, m, n);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        CotReceiverOutput[] cotReceiverOutput = new CotReceiverOutput[m];
        BigInteger[] result = IntStream.range(0, m).mapToObj(i -> BigInteger.ZERO).toArray(BigInteger[]::new);
        for (int i = 0; i < m; i++) {
            boolean choice = xs[m - i - 1];
            cotReceiverOutput[i] = cotReceiver[i].receive(new boolean[]{choice});
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime);

        DataPacketHeader encPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_ENC_ELEMENTS.ordinal(), extraInfo++,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        ArrayList<byte[]> encPayload = (ArrayList<byte[]>) rpc.receive(encPayloadHeader).getPayload();
        MpcAbortPreconditions.checkArgument(encPayload.size() == m);

        stopWatch.start();
        IntStream intStream = parallel ? IntStream.range(0, m).parallel() : IntStream.range(0, m);
        intStream.forEach(i -> {
            BigInteger t = BigIntegerUtils.byteArrayToBigInteger(cotReceiverOutput[i].getRb(0))
                .mod(outputZl.getRangeBound());
            if (cotReceiverOutput[i].getChoice(0)) {
                result[i] = t.subtract(BigIntegerUtils.byteArrayToBigInteger(encPayload.get(i)))
                    .mod(outputZl.getRangeBound());
            } else {
                result[i] = t.negate().mod(outputZl.getRangeBound());
            }
        });
        stopWatch.stop();
        long encTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, encTime);

        logPhaseInfo(PtoState.PTO_END);
        return Arrays.stream(result).reduce(BigInteger.ZERO, outputZl::add);
    }
}