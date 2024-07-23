package edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Abstract Zl Corr Party.
 *
 * @author Liqiang Peng
 * @date 2023/10/1
 */
public abstract class AbstractZlCorrParty extends AbstractTwoPartyPto implements ZlCorrParty {
    /**
     * max num
     */
    protected int maxNum;
    /**
     * max l
     */
    protected int maxL;
    /**
     * Zl instance
     */
    protected Zl zl;
    /**
     * l
     */
    protected int l;
    /**
     * l in bytes
     */
    protected int byteL;
    /**
     * num
     */
    protected int num;

    public AbstractZlCorrParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlCorrConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxL, int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        MathPreconditions.checkPositive("maxL", maxL);
        this.maxNum = maxNum;
        this.maxL = maxL;
        initState();
    }

    protected void setPtoInput(SquareZlVector xi) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", xi.getNum(), maxNum);
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl().getL(), maxL);
        num = xi.getNum();
        zl = xi.getZl();
        l = zl.getL();
        byteL = zl.getByteL();
    }
}
