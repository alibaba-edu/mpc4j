package edu.alibaba.mpc4j.s2pc.aby.operator.row.min2.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Abstract Zl Min2 Party.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public abstract class AbstractZlMin2Party extends AbstractTwoPartyPto implements ZlMin2Party {
    /**
     * max l
     */
    protected int maxL;
    /**
     * max num
     */
    protected int maxNum;
    /**
     * num
     */
    protected int num;

    public AbstractZlMin2Party(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlMin2Config config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxL, int maxNum) {
        MathPreconditions.checkPositive("maxL", maxL);
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxL = maxL;
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(SquareZlVector xi) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl().getL(), maxL);
        MathPreconditions.checkPositiveInRangeClosed("num", xi.getNum(), maxNum);
        num = xi.getNum();
    }
}
