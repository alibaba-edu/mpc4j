package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * abstract SSP-COT receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/13
 */
public abstract class AbstractSspCotReceiver extends AbstractTwoPartyPto implements SspCotReceiver {
    /**
     * config
     */
    protected final SspCotConfig config;
    /**
     * α
     */
    protected int alpha;
    /**
     * num
     */
    protected int num;
    /**
     * number of COTs
     */
    protected int cotNum;

    protected AbstractSspCotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, SspCotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int alpha, int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        cotNum = SspCotFactory.getPrecomputeNum(config, num);
        MathPreconditions.checkNonNegativeInRange("α", alpha, num);
        this.alpha = alpha;
        extraInfo++;
    }

    protected void setPtoInput(int alpha, int num, CotReceiverOutput preReceiverOutput) {
        setPtoInput(alpha, num);
        if (preReceiverOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preNum", preReceiverOutput.getNum(), SspCotFactory.getPrecomputeNum(config, num)
            );
        }
    }
}
