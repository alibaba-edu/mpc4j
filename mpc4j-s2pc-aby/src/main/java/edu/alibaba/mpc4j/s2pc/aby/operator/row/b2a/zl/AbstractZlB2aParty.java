package edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;

/**
 * Abstract Zl boolean to arithmetic protocol party.
 *
 * @author Liqiang Peng
 * @date 2024/6/4
 */
public abstract class AbstractZlB2aParty extends AbstractTwoPartyPto implements ZlB2aParty {
    /**
     * max num
     */
    protected int maxNum;
    /**
     * max l
     */
    protected int maxL;
    /**
     * num
     */
    protected int num;
    /**
     * zl
     */
    protected Zl zl;

    protected AbstractZlB2aParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    protected void setInitInput(int maxL, int maxNum) {
        MathPreconditions.checkPositive("maxL", maxL);
        this.maxL = maxL;
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(MpcZ2Vector xi, Zl zl) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("l", zl.getL(), maxL);
        this.zl = zl;
        MathPreconditions.checkPositiveInRangeClosed("inputs.num", xi.bitNum(), maxNum);
        num = xi.bitNum();
    }
}
