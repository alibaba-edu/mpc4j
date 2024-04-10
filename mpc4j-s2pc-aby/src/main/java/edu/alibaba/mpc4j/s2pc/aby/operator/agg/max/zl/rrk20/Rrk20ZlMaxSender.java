package edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.AbstractZlMaxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterParty;

import java.util.concurrent.TimeUnit;

/**
 * RRK+20 Zl Max Sender.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Rrk20ZlMaxSender extends AbstractZlMaxParty {
    /**
     * zl greater sender.
     */
    private final ZlGreaterParty zlGreaterSender;

    public Rrk20ZlMaxSender(Rpc senderRpc, Party receiverParty, Rrk20ZlMaxConfig config) {
        super(Rrk20ZlMaxPtoDesc.getInstance(), senderRpc, receiverParty, config);
        zlGreaterSender = ZlGreaterFactory.createSender(senderRpc, receiverParty, config.getZlGreaterConfig());
        addSubPto(zlGreaterSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        zlGreaterSender.init(maxL, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector max(SquareZlVector xi) throws MpcAbortException {
        setPtoInput(xi);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        SquareZlVector z = combine();
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, ptoTime);

        logPhaseInfo(PtoState.PTO_END);

        return z;
    }

    private SquareZlVector combine() throws MpcAbortException {
        int logNum = LongUtils.ceilLog2(num);
        int currentNodeNum = num / 2;
        int lastNodeNum = num;
        for (int i = 1; i <= logNum; i++) {
            for (int j = 0; j < currentNodeNum; j++) {
                inputs[j] = zlGreaterSender.gt(inputs[j * 2], inputs[j * 2 + 1]);
            }
            if (lastNodeNum % 2 == 1) {
                inputs[currentNodeNum] = inputs[lastNodeNum - 1];
                currentNodeNum++;
            }
            lastNodeNum = currentNodeNum;
            currentNodeNum = lastNodeNum / 2;
        }
        return inputs[0];
    }
}
