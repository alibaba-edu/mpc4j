package edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;

/**
 * abstract Zl edaBit generation party.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
public abstract class AbstractZlEdaBitGenParty extends AbstractTwoPartyPto implements ZlEdaBitGenParty {
    /**
     * Zl instance
     */
    protected final Zl zl;
    /**
     * l
     */
    protected final int l;
    /**
     * l in bytes
     */
    protected final int byteL;
    /**
     * max num
     */
    protected int maxNum;
    /**
     * num
     */
    protected int num;

    protected AbstractZlEdaBitGenParty(PtoDesc ptoDesc, Rpc ownPpc, Party otherParty, ZlEdaBitGenConfig config) {
        super(ptoDesc, ownPpc, otherParty, config);
        this.zl = config.getZl();
        l = zl.getL();
        byteL = zl.getByteL();
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int num) {
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        extraInfo++;
    }
}
