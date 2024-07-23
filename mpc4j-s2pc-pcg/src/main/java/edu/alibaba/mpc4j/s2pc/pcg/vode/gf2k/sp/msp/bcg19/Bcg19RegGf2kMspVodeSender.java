package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.bcg19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeSender;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.AbstractGf2kMspVodeSender;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeSenderOutput;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * BCG19-REG-MSP-VODE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Bcg19RegGf2kMspVodeSender extends AbstractGf2kMspVodeSender {
    /**
     * GF2K-BSP-VODE sender
     */
    private final Gf2kBspVodeSender gf2kBspVodeSender;
    /**
     * pre-computed sender output
     */
    private Gf2kVodeSenderOutput gf2kVodeSenderOutput;

    public Bcg19RegGf2kMspVodeSender(Rpc senderRpc, Party receiverParty, Bcg19RegGf2kMspVodeConfig config) {
        super(Bcg19RegGf2kMspVodePtoDesc.getInstance(), senderRpc, receiverParty, config);
        gf2kBspVodeSender = Gf2kBspVodeFactory.createSender(senderRpc, receiverParty, config.getGf2kBspVodeConfig());
        addSubPto(gf2kBspVodeSender);
    }

    @Override
    public void init(int subfieldL) throws MpcAbortException {
        setInitInput(subfieldL);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        gf2kBspVodeSender.init(subfieldL);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kMspVodeSenderOutput send(int t, int num) throws MpcAbortException {
        setPtoInput(t, num);
        return send();
    }

    @Override
    public Gf2kMspVodeSenderOutput send(int t, int num, Gf2kVodeSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(t, num, preSenderOutput);
        gf2kVodeSenderOutput = preSenderOutput;
        return send();
    }

    private Gf2kMspVodeSenderOutput send() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // generate sparse points
        int[] innerTargetArray = IntStream.range(0, t)
            .map(i -> {
                // due to the rounding problem, here we must convert to long then divide
                int lowerBound = (int) (i * (long) num / t);
                int upperBound = (int) ((i + 1) * (long) num / t);
                return secureRandom.nextInt(upperBound - lowerBound);
            })
            .toArray();
        // execute GF2K-BSP-VODE with batchNum = t, eachNum = num / t.
        Gf2kBspVodeSenderOutput gf2kBspVodeSenderOutput = gf2kBspVodeSender.send(
            innerTargetArray, (int) Math.ceil((double) num / t), gf2kVodeSenderOutput
        );
        gf2kVodeSenderOutput = null;
        stopWatch.stop();
        long bspTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, bspTime);

        stopWatch.start();
        Gf2kMspVodeSenderOutput senderOutput = generateSenderOutput(innerTargetArray, gf2kBspVodeSenderOutput);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private Gf2kMspVodeSenderOutput generateSenderOutput(int[] innerTargetArray,
                                                         Gf2kBspVodeSenderOutput gf2kBspVodeSenderOutput) {
        int[] alphaArray = new int[t];
        byte[][] alphaXs = new byte[t][];
        byte[][] ts = new byte[num][];
        IntStream.range(0, t).forEach(i -> {
            // due to the rounding problem, here we must convert to long then divide
            int lowerBound = (int) (i * (long) num / t);
            int upperBound = (int) ((i + 1) * (long) num / t);
            alphaArray[i] = innerTargetArray[i] + lowerBound;
            Gf2kSspVodeSenderOutput eachSenderOutput = gf2kBspVodeSenderOutput.get(i);
            alphaXs[i] = eachSenderOutput.getAlphaX();
            for (int j = 0; j < upperBound - lowerBound; j++) {
                ts[lowerBound + j] = eachSenderOutput.getT(j);
            }
        });
        return Gf2kMspVodeSenderOutput.create(field, alphaArray, alphaXs, ts);
    }
}
