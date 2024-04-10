package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.AbstractNcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * direct no-choice COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/7/14
 */
public class DirectNcCotReceiver extends AbstractNcCotReceiver {
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;

    public DirectNcCotReceiver(Rpc receiverRpc, Party senderParty, DirectNcCotConfig config) {
        super(DirectNcCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
    }

    @Override
    public void init(int num) throws MpcAbortException {
        setInitInput(num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotReceiver.init(num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public CotReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // randomly generate choice bits.
        boolean[] choices = new boolean[num];
        IntStream.range(0, num).forEach(index -> choices[index] = secureRandom.nextBoolean());
        CotReceiverOutput receiverOutput = coreCotReceiver.receive(choices);
        stopWatch.stop();
        long coreCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, coreCotTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
