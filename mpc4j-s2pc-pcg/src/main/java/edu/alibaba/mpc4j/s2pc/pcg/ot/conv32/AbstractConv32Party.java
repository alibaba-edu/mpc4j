package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * abstract F_3 -> F_2 modulus conversion party.
 *
 * @author Weiran Liu
 * @date 2024/6/5
 */
public abstract class AbstractConv32Party extends AbstractTwoPartyPto implements Conv32Party {
    /**
     * config
     */
    protected Conv32Config config;
    /**
     * expect num
     */
    protected int expectNum;
    /**
     * wi ∈ F_3
     */
    protected byte[] wi;
    /**
     * num
     */
    protected int num;

    protected AbstractConv32Party(PtoDesc ptoDesc, Rpc rpc, Party otherParty, Conv32Config config) {
        super(ptoDesc, rpc, otherParty, config);
        this.config = config;
    }

    protected void setInitInput(int expectNum) {
        MathPreconditions.checkPositive("expect_num", expectNum);
        this.expectNum = expectNum;
        initState();
    }

    protected void setPtoInput(byte[] wi) {
        checkInitialized();
        MathPreconditions.checkPositive("num", wi.length);
        num = wi.length;
        for (int i = 0; i < num; i++) {
            MathPreconditions.checkNonNegativeInRange("wi[" + i + "]", wi[i], 3);
        }
        this.wi = wi;
    }
}
