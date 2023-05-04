package edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * abstract sing-point DPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public abstract class AbstractSpDpprfSender extends AbstractTwoPartyPto implements SpDpprfSender {
    /**
     * config
     */
    protected final SpDpprfConfig config;
    /**
     * max α bound
     */
    protected int maxAlphaBound;
    /**
     * max α bit length
     */
    protected int maxH;
    /**
     * α bound
     */
    protected int alphaBound;
    /**
     * α bit length
     */
    protected int h;

    protected AbstractSpDpprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, SpDpprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(int maxAlphaBound) {
        MathPreconditions.checkPositive("maxAlphaBound", maxAlphaBound);
        this.maxAlphaBound = maxAlphaBound;
        maxH = LongUtils.ceilLog2(maxAlphaBound, 1);
        initState();
    }

    protected void setPtoInput(int alphaBound) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("alphaBound", alphaBound, maxAlphaBound);
        this.alphaBound = alphaBound;
        h = LongUtils.ceilLog2(alphaBound, 1);
        extraInfo++;
    }

    protected void setPtoInput(int alphaBound, CotSenderOutput preSenderOutput) {
        setPtoInput(alphaBound);
        MathPreconditions.checkGreaterOrEqual(
            "preCotNum", preSenderOutput.getNum(), SpDpprfFactory.getPrecomputeNum(config, alphaBound)
        );
    }
}
