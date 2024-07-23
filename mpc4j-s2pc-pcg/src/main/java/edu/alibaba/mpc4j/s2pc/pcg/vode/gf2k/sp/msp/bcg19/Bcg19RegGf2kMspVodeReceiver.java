package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.bcg19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.AbstractGf2kMspVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeReceiverOutput;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * BCG19-REG-MSP-VODE receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Bcg19RegGf2kMspVodeReceiver extends AbstractGf2kMspVodeReceiver {
    /**
     * GF2K-BSP-VODE receiver
     */
    private final Gf2kBspVodeReceiver gf2kBspVodeReceiver;
    /**
     * pre-computed receiver output
     */
    private Gf2kVodeReceiverOutput gf2kVodeReceiverOutput;

    public Bcg19RegGf2kMspVodeReceiver(Rpc receiverRpc, Party senderParty, Bcg19RegGf2kMspVodeConfig config) {
        super(Bcg19RegGf2kMspVodePtoDesc.getInstance(), receiverRpc, senderParty, config);
        gf2kBspVodeReceiver = Gf2kBspVodeFactory.createReceiver(receiverRpc, senderParty, config.getGf2kBspVodeConfig());
        addSubPto(gf2kBspVodeReceiver);
    }

    @Override
    public void init(int subfieldL, byte[] delta) throws MpcAbortException {
        setInitInput(subfieldL, delta);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        gf2kBspVodeReceiver.init(subfieldL, delta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kMspVodeReceiverOutput receive(int t, int num) throws MpcAbortException {
        setPtoInput(t, num);
        return receive();
    }

    @Override
    public Gf2kMspVodeReceiverOutput receive(int t, int num, Gf2kVodeReceiverOutput preReceiverOutput)
        throws MpcAbortException {
        setPtoInput(t, num, preReceiverOutput);
        gf2kVodeReceiverOutput = preReceiverOutput;
        return receive();
    }

    private Gf2kMspVodeReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // execute GF2K-BSP-VODE with batchNum = t, eachNum = num / t.
        Gf2kBspVodeReceiverOutput gf2kBspVodeReceiverOutput = gf2kBspVodeReceiver.receive(
            t, (int) Math.ceil((double) num / t), gf2kVodeReceiverOutput
        );
        gf2kVodeReceiverOutput = null;
        stopWatch.stop();
        long bspTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, bspTime);

        stopWatch.start();
        Gf2kMspVodeReceiverOutput receiverOutput = generateReceiverOutput(gf2kBspVodeReceiverOutput);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private Gf2kMspVodeReceiverOutput generateReceiverOutput(final Gf2kBspVodeReceiverOutput gf2kBspVodeReceiverOutput) {
        byte[][] qs = IntStream.range(0, t)
            .mapToObj(i -> {
                // we need to first compute num / t then multiply i, since i * num may be greater than Integer.MAX_VALUE
                // due to the rounding problem, here we must convert to long then divide
                int lowerBound = (int) (i * (long) num / t);
                int upperBound = (int) ((i + 1) * (long) num / t);
                Gf2kSspVodeReceiverOutput eachReceiverOutput = gf2kBspVodeReceiverOutput.get(i);
                return IntStream.range(0, upperBound - lowerBound)
                    .mapToObj(eachReceiverOutput::getQ)
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
        return Gf2kMspVodeReceiverOutput.create(field, delta, qs);
    }
}
