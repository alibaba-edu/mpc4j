package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.plg24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.ZlDaBitTuple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.AbstractZlDaBitGenParty;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.plg24.Plg24ZlDaBitGenPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * PLG24 Zl daBit generation receiver.
 *
 * @author Weiran Liu
 * @date 2024/7/2
 */
public class Plg24ZlDaBitGenReceiver extends AbstractZlDaBitGenParty {
    /**
     * default round num
     */
    private final int defaultRounNum;
    /**
     * COT receiver
     */
    private final CotReceiver cotReceiver;

    public Plg24ZlDaBitGenReceiver(Rpc receiverRpc, Party senderParty, Plg24ZlDaBitGenConfig config) {
        super(Plg24ZlDaBitGenPtoDesc.getInstance(), receiverRpc, senderParty, config);
        CotConfig cotConfig = config.getCotConfig();
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, cotConfig);
        addSubPto(cotReceiver);
        defaultRounNum = cotConfig.defaultRoundNum();
    }

    @Override
    public void init(int maxL, int expectTotalNum) throws MpcAbortException {
        setInitInput(maxL, expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        cotReceiver.init(expectTotalNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int maxL) throws MpcAbortException {
        init(maxL, defaultRounNum);
    }

    @Override
    public ZlDaBitTuple generate(Zl zl, int num) throws MpcAbortException {
        setPtoInput(zl, num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = cotReceiver.receiveRandom(num);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime);

        List<byte[]> correlationPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_CORRELATION.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(correlationPayload.size() == num);
        byte[][] correlationBytes = correlationPayload.toArray(new byte[0][]);
        ZlVector s1Vector = ZlVector.createZeros(zl, num);
        IntStream indexIntStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
        indexIntStream.forEach(i -> {
                BigInteger hrb = zl.createRandom(cotReceiverOutput.getRb(i));
                if (cotReceiverOutput.getChoice(i)) {
                    // P1 sets m_1 = m_b + H(r1) = H(r0) + 1 + 2 * [r]_0^2.
                    BigInteger mb = BigIntegerUtils.byteArrayToNonNegBigInteger(correlationBytes[i]);
                    s1Vector.setElement(i, zl.neg(zl.add(hrb, mb)));
                } else {
                    // P1 sets m_0 = H(r0)
                    s1Vector.setElement(i, zl.neg(hrb));
                }
            });
        BitVector r1Vector = BitVectorFactory.create(num, BinaryUtils.binaryToRoundByteArray(cotReceiverOutput.getChoices()));
        stopWatch.stop();
        long z2Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, z2Time);

        logPhaseInfo(PtoState.PTO_END);
        return ZlDaBitTuple.create(SquareZlVector.create(s1Vector, false), SquareZ2Vector.create(r1Vector, false));
    }
}
