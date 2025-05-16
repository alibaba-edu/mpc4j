package edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.rrgg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.AbstractZlCrossTermReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.rrgg21.Rrgg21ZlCrossTermPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.rrgg21.Rrgg21ZlCrossTermPtoDesc.getInstance;

/**
 * RRGG21 Zl Cross Term Multiplication Receiver.
 *
 * @author Liqiang Peng
 * @date 2024/6/5
 */
public class Rrgg21ZlCrossTermReceiver extends AbstractZlCrossTermReceiver {
    /**
     * cot sender
     */
    private final CotSender[] cotSender;

    public Rrgg21ZlCrossTermReceiver(Z2cParty z2cReceiver, Party senderParty, Rrgg21ZlCrossTermConfig config) {
        super(getInstance(), z2cReceiver.getRpc(), senderParty, config);
        cotSender = new CotSender[Long.SIZE];
        IntStream.range(0, Long.SIZE).forEach(i -> {
            cotSender[i] = CotFactory.createSender(z2cReceiver.getRpc(), senderParty, config.getCotConfig());
            addSubPto(cotSender[i]);
        });
    }

    @Override
    public void init(int maxM, int maxN) throws MpcAbortException {
        setInitInput(maxM, maxN);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        for (int i = 0; i < Long.SIZE; i++) {
            byte[] delta = BlockUtils.randomBlock(secureRandom);
            cotSender[i].init(delta, 1);
        }
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BigInteger crossTerm(BigInteger y, int m, int n) throws MpcAbortException {
        setPtoInput(y, m, n);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        CotSenderOutput[] cotSenderOutput = new CotSenderOutput[m];
        for (int i = 0; i < m; i++) {
            cotSenderOutput[i] = cotSender[i].send(1);
        }
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime);

        stopWatch.start();
        BigInteger[] result = IntStream.range(0, m)
            .mapToObj(i ->
                BigIntegerUtils.byteArrayToBigInteger(cotSenderOutput[i].getR0(0))
                    .mod(outputZl.getRangeBound()))
            .toArray(BigInteger[]::new);
        IntStream intStream = IntStream.range(0, m);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> encPayload = intStream
            .mapToObj(i -> BigIntegerUtils.byteArrayToBigInteger(cotSenderOutput[i].getR1(0))
                .subtract(y.shiftLeft(i).subtract(result[i]))
                .mod(outputZl.getRangeBound()))
            .map(BigIntegerUtils::bigIntegerToByteArray)
            .collect(Collectors.toList());
        long encTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, encTime);

        DataPacketHeader encPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_ENC_ELEMENTS.ordinal(), extraInfo++,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(encPayloadHeader, encPayload));

        logPhaseInfo(PtoState.PTO_END);
        return Arrays.stream(result).reduce(BigInteger.ZERO, outputZl::add);
    }
}
