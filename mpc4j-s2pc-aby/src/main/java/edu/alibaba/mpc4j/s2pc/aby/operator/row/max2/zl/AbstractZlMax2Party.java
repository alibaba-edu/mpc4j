package edu.alibaba.mpc4j.s2pc.aby.operator.row.max2.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Abstract Zl Max2 Party.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public abstract class AbstractZlMax2Party extends AbstractTwoPartyPto implements ZlMax2Party {
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
     * l in bytes
     */
    protected int byteL;

    public AbstractZlMax2Party(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlMax2Config config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxL, int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        this.maxL = maxL;
        initState();
    }

    protected void setPtoInput(SquareZlVector xi) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", xi.getNum(), maxNum);
        num = xi.getNum();
    }
}
