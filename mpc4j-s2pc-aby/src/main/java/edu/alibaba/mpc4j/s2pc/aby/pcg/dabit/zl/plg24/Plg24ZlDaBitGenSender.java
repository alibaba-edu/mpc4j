package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.plg24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
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
 * PLG24 Zl daBit generation sender.
 *
 * @author Weiran Liu
 * @date 2024/7/2
 */
public class Plg24ZlDaBitGenSender extends AbstractZlDaBitGenParty {
    /**
     * default round num
     */
    private final int defaultRounNum;
    /**
     * COT sender
     */
    private final CotSender cotSender;

    public Plg24ZlDaBitGenSender(Rpc senderRpc, Party receiverParty, Plg24ZlDaBitGenConfig config) {
        super(Plg24ZlDaBitGenPtoDesc.getInstance(), senderRpc, receiverParty, config);
        CotConfig cotConfig = config.getCotConfig();
        cotSender = CotFactory.createSender(senderRpc, receiverParty, cotConfig);
        addSubPto(cotSender);
        defaultRounNum = cotConfig.defaultRoundNum();
    }

    @Override
    public void init(int maxL, int expectTotalNum) throws MpcAbortException {
        setInitInput(maxL, expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = BlockUtils.randomBlock(secureRandom);
        cotSender.init(delta, expectTotalNum);
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
        CotSenderOutput cotSenderOutput = cotSender.sendRandom(num);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime);

        stopWatch.start();
        // P0 samples [r]_0^2.
        BitVector r0Vector = BitVectorFactory.createRandom(num, secureRandom);
        // P0 computes m_0 = H(r0).
        // P0 knows [s]_0^p = m_0 + [r]_0^2, so that m_1 = [s]_0^p - (1 - [r]_0^2) = H(r0) + 2 * [r]_0^2 - 1
        // P0 sets m_b = m_1 - H(r1) = H(r0) + 2 * [r]_0^2 - 1 - H(r1)
        ZlVector s0Vector = ZlVector.createZeros(zl, num);
        IntStream indexIntStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
        List<byte[]> correlationPayload = indexIntStream
            .mapToObj(i -> {
                BigInteger hr0 = zl.createRandom(cotSenderOutput.getR0(i));
                BigInteger hr1 = zl.createRandom(cotSenderOutput.getR1(i));
                if (r0Vector.get(i)) {
                    s0Vector.setElement(i, zl.add(hr0, zl.createOne()));
                    return zl.sub(zl.add(hr0, zl.createOne()), hr1);
                } else {
                    s0Vector.setElement(i, hr0);
                    return zl.sub(zl.sub(hr0, zl.createOne()), hr1);
                }
            })
            .map(correlation -> BigIntegerUtils.nonNegBigIntegerToByteArray(correlation, byteL))
            .toList();
        sendOtherPartyPayload(PtoStep.RECEIVER_SEND_CORRELATION.ordinal(), correlationPayload);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        // P0 sets [t]_0^p = [s]_0^p
        return ZlDaBitTuple.create(SquareZlVector.create(s0Vector, false), SquareZ2Vector.create(r0Vector, false));
    }
}
