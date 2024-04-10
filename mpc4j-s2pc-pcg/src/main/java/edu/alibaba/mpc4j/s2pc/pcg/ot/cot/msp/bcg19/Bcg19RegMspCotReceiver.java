package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.bcg19;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.SspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.AbstractMspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotReceiverOutput;

/**
 * BCG19-REG-MSP-COT receiver
 *
 * @author Weiran Liu
 * @date 2022/01/25
 */
public class Bcg19RegMspCotReceiver extends AbstractMspCotReceiver {
    /**
     * BSP-COT receiver
     */
    private final BspCotReceiver bspCotReceiver;
    /**
     * pre-computed COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;

    public Bcg19RegMspCotReceiver(Rpc senderRpc, Party receiverParty, Bcg19RegMspCotConfig config) {
        super(Bcg19RegMspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bspCotReceiver = BspCotFactory.createReceiver(senderRpc, receiverParty, config.getBspCotConfig());
        addSubPto(bspCotReceiver);
    }

    @Override
    public void init(int maxT, int maxNum) throws MpcAbortException {
        setInitInput(maxT, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // In theory, maxEachNum = maxN / maxT, but for larger T, maxN / maxT can be small. So we set maxEachNum = maxN
        bspCotReceiver.init(maxT, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MspCotReceiverOutput receive(int t, int num) throws MpcAbortException {
        setPtoInput(t, num);
        return receive();
    }

    @Override
    public MspCotReceiverOutput receive(int t, int num, CotReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(t, num, preReceiverOutput);
        cotReceiverOutput = preReceiverOutput;
        return receive();
    }

    private MspCotReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // generate sparse points
        int[] innerAlphaArray = IntStream.range(0, t)
            .map(i -> {
                // due to the rounding problem, here we must convert to long then divide
                int lowerBound = (int) (i * (long) num / t);
                int upperBound = (int) ((i + 1) * (long) num / t);
                return secureRandom.nextInt(upperBound - lowerBound);
            })
            .toArray();
        // execute BSP-COT with batchNum = t, eachNum = num / t.
        BspCotReceiverOutput bspCotReceiverOutput;
        if (cotReceiverOutput == null) {
            bspCotReceiverOutput = bspCotReceiver.receive(innerAlphaArray, (int) Math.ceil((double) num / t));
        } else {
            bspCotReceiverOutput = bspCotReceiver.receive(
                innerAlphaArray, (int) Math.ceil((double) num / t), cotReceiverOutput
            );
            cotReceiverOutput = null;
        }
        stopWatch.stop();
        long bspTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, bspTime);

        stopWatch.start();
        MspCotReceiverOutput receiverOutput = generateReceiverOutput(innerAlphaArray, bspCotReceiverOutput);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private MspCotReceiverOutput generateReceiverOutput(int[] innerAlphaArray,
                                                        BspCotReceiverOutput bspCotReceiverOutput) {
        int[] alphaArray = new int[t];
        byte[][] rbArray = IntStream.range(0, t)
            .mapToObj(i -> {
                // we need to first compute num / t then multiply i, since i * num may be greater than Integer.MAX_VALUE
                // due to the rounding problem, here we must convert to long then divide
                int lowerBound = (int) (i * (long) num / t);
                int upperBound = (int) ((i + 1) * (long) num / t);
                alphaArray[i] = innerAlphaArray[i] + lowerBound;
                SspCotReceiverOutput eachReceiverOutput = bspCotReceiverOutput.get(i);
                return IntStream.range(0, upperBound - lowerBound)
                    .mapToObj(eachReceiverOutput::getRb)
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
        return MspCotReceiverOutput.create(alphaArray, rbArray);
    }
}
