package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.bcg19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.AbstractGf2kMspVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleReceiverOutput;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * BCG19-REG-MSP-COT receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
public class Bcg19RegGf2kMspVoleReceiver extends AbstractGf2kMspVoleReceiver {
    /**
     * GF2K-BSP-VOLE receiver
     */
    private final Gf2kBspVoleReceiver gf2kBspVoleReceiver;
    /**
     * pre-computed GF2K-BSP-VOLE receiver output
     */
    private Gf2kVoleReceiverOutput gf2kVoleReceiverOutput;

    public Bcg19RegGf2kMspVoleReceiver(Rpc receiverRpc, Party senderParty, Bcg19RegGf2kMspVoleConfig config) {
        super(Bcg19RegGf2kMspVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
        gf2kBspVoleReceiver = Gf2kBspVoleFactory.createReceiver(receiverRpc, senderParty, config.getGf2kBspVoleConfig());
        addSubPto(gf2kBspVoleReceiver);
    }

    @Override
    public void init(byte[] delta, int maxT, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxT, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // In theory, maxEachNum = maxN / maxT, but for larger T, maxN / maxT can be small. So we set maxEachNum = maxN
        gf2kBspVoleReceiver.init(delta, maxT, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kMspVoleReceiverOutput receive(int t, int num) throws MpcAbortException {
        setPtoInput(t, num);
        return receive();
    }

    @Override
    public Gf2kMspVoleReceiverOutput receive(int t, int num, Gf2kVoleReceiverOutput preReceiverOutput)
        throws MpcAbortException {
        setPtoInput(t, num, preReceiverOutput);
        gf2kVoleReceiverOutput = preReceiverOutput;
        return receive();
    }

    private Gf2kMspVoleReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // execute GF2K-BSP-VOLE with batchNum = t, eachNum = num / t.
        Gf2kBspVoleReceiverOutput gf2kBspVoleReceiverOutput;
        if (gf2kVoleReceiverOutput == null) {
            gf2kBspVoleReceiverOutput = gf2kBspVoleReceiver.receive(t, (int) Math.ceil((double) num / t));
        } else {
            gf2kBspVoleReceiverOutput = gf2kBspVoleReceiver.receive(t, (int) Math.ceil((double) num / t), gf2kVoleReceiverOutput);
            gf2kVoleReceiverOutput = null;
        }
        stopWatch.stop();
        long bspTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, bspTime);

        stopWatch.start();
        Gf2kMspVoleReceiverOutput receiverOutput = generateReceiverOutput(gf2kBspVoleReceiverOutput);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private Gf2kMspVoleReceiverOutput generateReceiverOutput(final Gf2kBspVoleReceiverOutput gf2kBspVoleReceiverOutput) {
        byte[][] qs = IntStream.range(0, t)
            .mapToObj(i -> {
                // we need to first compute num / t then multiply i, since i * num may be greater than Integer.MAX_VALUE
                // due to the rounding problem, here we must convert to long then divide
                int lowerBound = (int) (i * (long) num / t);
                int upperBound = (int) ((i + 1) * (long) num / t);
                Gf2kSspVoleReceiverOutput eachReceiverOutput = gf2kBspVoleReceiverOutput.get(i);
                return IntStream.range(0, upperBound - lowerBound)
                    .mapToObj(eachReceiverOutput::getQ)
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
        return Gf2kMspVoleReceiverOutput.create(delta, qs);
    }
}
