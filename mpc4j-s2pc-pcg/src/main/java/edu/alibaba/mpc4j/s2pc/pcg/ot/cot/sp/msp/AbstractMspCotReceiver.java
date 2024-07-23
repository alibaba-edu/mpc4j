package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * abstract multi single-point COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public abstract class AbstractMspCotReceiver extends AbstractTwoPartyPto implements MspCotReceiver {
    /**
     * config
     */
    private final MspCotConfig config;
    /**
     * num
     */
    protected int num;
    /**
     * sparse num
     */
    protected int t;

    protected AbstractMspCotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, MspCotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int t, int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        MathPreconditions.checkPositiveInRangeClosed("t", t, num);
        this.t = t;
        extraInfo++;
    }

    protected void setPtoInput(int t, int num, CotReceiverOutput preReceiverOutput) {
        setPtoInput(t, num);
        if (preReceiverOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preCotNum", preReceiverOutput.getNum(), MspCotFactory.getPrecomputeNum(config, t, num)
            );
        }
    }
}
