package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;

/**
 * abstract Zl daBit generation party.
 *
 * @author Weiran Liu
 * @date 2023/5/18
 */
public abstract class AbstractZlDaBitGenParty extends AbstractTwoPartyPto implements ZlDaBitGenParty {
    /**
     * max l
     */
    protected int maxL;
    /**
     * Zl
     */
    protected Zl zl;
    /**
     * l
     */
    protected int l;
    /**
     * byte l
     */
    protected int byteL;
    /**
     * num
     */
    protected int num;

    protected AbstractZlDaBitGenParty(PtoDesc ptoDesc, Rpc ownPpc, Party otherParty, ZlDaBitGenConfig config) {
        super(ptoDesc, ownPpc, otherParty, config);
    }

    protected void setInitInput(int maxL, int expectTotalNum) {
        MathPreconditions.checkPositiveInRangeClosed("maxL", maxL, Long.SIZE);
        MathPreconditions.checkPositive("expect_num", expectTotalNum);
        this.maxL = maxL;
        initState();
    }

    protected void setInitInput(int maxL) {
        MathPreconditions.checkPositive("maxL", maxL);
        this.maxL = maxL;
        initState();
    }

    protected void setPtoInput(Zl zl, int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("l", zl.getL(), maxL);
        this.zl = zl;
        l = zl.getL();
        byteL = zl.getByteL();
        this.num = num;
    }
}
