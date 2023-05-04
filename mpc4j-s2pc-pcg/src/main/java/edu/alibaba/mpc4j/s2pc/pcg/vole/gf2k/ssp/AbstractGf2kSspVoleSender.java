package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * abstract single single-point GF2K VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public abstract class AbstractGf2kSspVoleSender extends AbstractTwoPartyPto implements Gf2kSspVoleSender {
    /**
     * config
     */
    protected final Gf2kSspVoleConfig config;
    /**
     * max num
     */
    protected int maxNum;
    /**
     * α
     */
    protected int alpha;
    /**
     * num
     */
    protected int num;

    protected AbstractGf2kSspVoleSender(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kSspVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int alpha, int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        MathPreconditions.checkNonNegativeInRange("α", alpha, num);
        this.alpha = alpha;
        extraInfo++;
    }

    protected void setPtoInput(int alpha, int num, Gf2kVoleSenderOutput preSenderOutput) {
        setPtoInput(alpha, num);
        MathPreconditions.checkGreaterOrEqual(
            "preNum", preSenderOutput.getNum(), Gf2kSspVoleFactory.getPrecomputeNum(config, num)
        );
    }
}
