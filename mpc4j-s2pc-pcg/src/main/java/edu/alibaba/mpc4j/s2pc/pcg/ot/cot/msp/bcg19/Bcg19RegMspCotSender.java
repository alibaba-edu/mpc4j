package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.bcg19;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.SspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.AbstractMspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotSenderOutput;

/**
 * BCG19-REG-MSP-COT sender.
 *
 * @author Weiran Liu
 * @date 2022/01/25
 */
public class Bcg19RegMspCotSender extends AbstractMspCotSender {
    /**
     * BSP-COT sender
     */
    private final BspCotSender bspCotSender;
    /**
     * pre-computed COT sender output
     */
    private CotSenderOutput cotSenderOutput;

    public Bcg19RegMspCotSender(Rpc senderRpc, Party receiverParty, Bcg19RegMspCotConfig config) {
        super(Bcg19RegMspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bspCotSender = BspCotFactory.createSender(senderRpc, receiverParty, config.getBspCotConfig());
        addSubPto(bspCotSender);
    }

    @Override
    public void init(byte[] delta, int maxT, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxT, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // In theory, maxEachNum = maxN / maxT, but for larger T, maxN / maxT can be small. So we set maxEachNum = maxN
        bspCotSender.init(delta, maxT, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MspCotSenderOutput send(int t, int num) throws MpcAbortException {
        setPtoInput(t, num);
        return send();
    }

    @Override
    public MspCotSenderOutput send(int t, int num, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(t, num, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return send();
    }

    private MspCotSenderOutput send() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // execute BSP-COT with batchNum = t, eachNum = num / t.
        BspCotSenderOutput bspCotSenderOutput;
        if (cotSenderOutput == null) {
            bspCotSenderOutput = bspCotSender.send(t, (int) Math.ceil((double) num / t));
        } else {
            bspCotSenderOutput = bspCotSender.send(t, (int) Math.ceil((double) num / t), cotSenderOutput);
            cotSenderOutput = null;
        }
        stopWatch.stop();
        long bspTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, bspTime);

        stopWatch.start();
        MspCotSenderOutput senderOutput = generateSenderOutput(bspCotSenderOutput);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private MspCotSenderOutput generateSenderOutput(BspCotSenderOutput bspCotSenderOutput) {
        byte[][] r0Array = IntStream.range(0, t)
            .mapToObj(i -> {
                // we need to first compute num / t then multiply i, since i * num may be greater than Integer.MAX_VALUE
                // due to the rounding problem, here we must convert to long then divide
                int lowerBound = (int) (i * (long) num / t);
                int upperBound = (int) ((i + 1) * (long) num / t);
                SspCotSenderOutput eachSenderOutput = bspCotSenderOutput.get(i);
                return IntStream.range(0, upperBound - lowerBound)
                    .mapToObj(eachSenderOutput::getR0)
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
        return MspCotSenderOutput.create(delta, r0Array);
    }
}
