package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.AbstractCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;

import java.util.concurrent.TimeUnit;

/**
 * direct COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class DirectCotReceiver extends AbstractCotReceiver {
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * round num
     */
    private int roundNum;

    public DirectCotReceiver(Rpc receiverRpc, Party senderParty, DirectCotConfig config) {
        super(DirectCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
    }

    @Override
    public void init(int expectNum) throws MpcAbortException {
        setInitInput(expectNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        roundNum = Math.min(DirectCotPtoDesc.MAX_ROUND_NUM, expectNum);
        coreCotReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init() throws MpcAbortException {
        init(DirectCotPtoDesc.MAX_ROUND_NUM);
    }

    @Override
    public CotReceiverOutput receive(boolean[] choices) throws MpcAbortException {
        setPtoInput(choices);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        CotReceiverOutput receiverOutput = CotReceiverOutput.createEmpty();
        while (num > receiverOutput.getNum()) {
            int gapNum = num - receiverOutput.getNum();
            int eachNum = Math.min(gapNum, roundNum);
            boolean[] roundChoices = new boolean[eachNum];
            System.arraycopy(choices, receiverOutput.getNum(), roundChoices, 0, eachNum);
            receiverOutput.merge(coreCotReceiver.receive(roundChoices));
        }
        stopWatch.stop();
        long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, roundTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    @Override
    public CotReceiverOutput receiveRandom(int num) throws MpcAbortException {
        return receive(BinaryUtils.randomBinary(num, secureRandom));
    }
}
