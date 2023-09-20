package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * abstract GF2K-MSP-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
public abstract class AbstractGf2kMspVoleSender extends AbstractTwoPartyPto implements Gf2kMspVoleSender {
    /**
     * config
     */
    private final Gf2kMspVoleConfig config;
    /**
     * max num
     */
    private int maxNum;
    /**
     * max sparse num
     */
    private int maxT;
    /**
     * num
     */
    protected int num;
    /**
     * sparse num
     */
    protected int t;

    protected AbstractGf2kMspVoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Gf2kMspVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(int maxT, int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        MathPreconditions.checkPositiveInRangeClosed("maxT", maxT, maxNum);
        this.maxT = maxT;
        initState();
    }

    protected void setPtoInput(int t, int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        MathPreconditions.checkPositiveInRangeClosed("t", t, num);
        MathPreconditions.checkPositiveInRangeClosed("t", t, maxT);
        this.t = t;
        extraInfo++;
    }

    protected void setPtoInput(int t, int num, Gf2kVoleSenderOutput preSenderOutput) {
        setPtoInput(t, num);
        MathPreconditions.checkGreaterOrEqual(
            "preNum", preSenderOutput.getNum(), Gf2kMspVoleFactory.getPrecomputeNum(config, t, num)
        );
    }
}
