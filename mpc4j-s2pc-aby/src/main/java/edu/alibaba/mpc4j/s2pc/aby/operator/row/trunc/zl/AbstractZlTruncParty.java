package edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Abstract Zl Truncation Party.
 *
 * @author Liqiang Peng
 * @date 2023/10/1
 */
public abstract class AbstractZlTruncParty extends AbstractTwoPartyPto implements ZlTruncParty {
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
    /**
     * Zl instance
     */
    protected Zl zl;
    /**
     * l.
     */
    protected int l;
    /**
     * l in bytes
     */
    protected int byteL;

    public AbstractZlTruncParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlTruncConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxL, int maxNum) {
        MathPreconditions.checkPositive("maxL", maxL);
        this.maxL = maxL;
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(SquareZlVector xi, int s) {
        MathPreconditions.checkPositive("shift bit", s);
        num = xi.getNum();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        zl = xi.getZl();
        l = zl.getL();
        MathPreconditions.checkPositiveInRangeClosed("l", l, maxL);
        byteL = zl.getByteL();
    }
}
