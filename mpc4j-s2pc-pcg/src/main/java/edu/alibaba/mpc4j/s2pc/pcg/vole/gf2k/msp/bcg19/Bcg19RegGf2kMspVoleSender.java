package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.bcg19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.AbstractGf2kMspVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleSenderOutput;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * BCG19-REG-MSP-COT sender.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
public class Bcg19RegGf2kMspVoleSender extends AbstractGf2kMspVoleSender {
    /**
     * GF2K-BSP-VOLE sender
     */
    private final Gf2kBspVoleSender gf2kBspVoleSender;
    /**
     * pre-computed GF2K-VOLE sender output
     */
    private Gf2kVoleSenderOutput gf2kVoleSenderOutput;

    public Bcg19RegGf2kMspVoleSender(Rpc senderRpc, Party receiverParty, Bcg19RegGf2kMspVoleConfig config) {
        super(Bcg19RegGf2kMspVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        gf2kBspVoleSender = Gf2kBspVoleFactory.createSender(senderRpc, receiverParty, config.getGf2kBspVoleConfig());
        addSubPto(gf2kBspVoleSender);
    }

    @Override
    public void init(int maxT, int maxNum) throws MpcAbortException {
        setInitInput(maxT, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // In theory, maxEachNum = maxN / maxT, but for larger T, maxN / maxT can be small. So we set maxEachNum = maxN
        gf2kBspVoleSender.init(maxT, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kMspVoleSenderOutput send(int t, int num) throws MpcAbortException {
        setPtoInput(t, num);
        return send();
    }

    @Override
    public Gf2kMspVoleSenderOutput send(int t, int num, Gf2kVoleSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(t, num, preSenderOutput);
        gf2kVoleSenderOutput = preSenderOutput;
        return send();
    }

    private Gf2kMspVoleSenderOutput send() throws MpcAbortException {
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
        // execute GF2K-BSP-VOLE with batchNum = t, eachNum = num / t.
        Gf2kBspVoleSenderOutput gf2kBspVoleSenderOutput;
        if (gf2kVoleSenderOutput == null) {
            gf2kBspVoleSenderOutput = gf2kBspVoleSender.send(innerTargetArray, (int) Math.ceil((double) num / t));
        } else {
            gf2kBspVoleSenderOutput = gf2kBspVoleSender.send(
                innerTargetArray, (int) Math.ceil((double) num / t), gf2kVoleSenderOutput
            );
            gf2kVoleSenderOutput = null;
        }
        stopWatch.stop();
        long bspTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, bspTime);

        stopWatch.start();
        // 计算输出结果
        Gf2kMspVoleSenderOutput senderOutput = generateSenderOutput(innerTargetArray, gf2kBspVoleSenderOutput);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private Gf2kMspVoleSenderOutput generateSenderOutput(int[] innerTargetArray,
                                                         Gf2kBspVoleSenderOutput gf2kBspVoleSenderOutput) {
        int[] alphaArray = new int[t];
        byte[][] alphaXs = new byte[t][];
        byte[][] ts = new byte[num][];
        IntStream.range(0, t).forEach(i -> {
            // due to the rounding problem, here we must convert to long then divide
            int lowerBound = (int) (i * (long) num / t);
            int upperBound = (int) ((i + 1) * (long) num / t);
            alphaArray[i] = innerTargetArray[i] + lowerBound;
            Gf2kSspVoleSenderOutput eachSenderOutput = gf2kBspVoleSenderOutput.get(i);
            alphaXs[i] = eachSenderOutput.getAlphaX();
            for (int j = 0; j < upperBound - lowerBound; j++) {
                ts[lowerBound + j] = eachSenderOutput.getT(j);
            }
        });
        return Gf2kMspVoleSenderOutput.create(alphaArray, alphaXs, ts);
    }
}
