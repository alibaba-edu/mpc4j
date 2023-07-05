package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;

/**
 * abstract Zl multiplication triple generator.
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public abstract class AbstractZlMtgParty extends AbstractTwoPartyPto implements ZlMtgParty {
    /**
     * Zl instance
     */
    protected final Zl zl;
    /**
     * 比特长度
     */
    protected final int l;
    /**
     * update num
     */
    protected long updateNum;
    /**
     * num
     */
    protected int num;

    public AbstractZlMtgParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlMtgConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        zl = config.getZl();
        l = zl.getL();
    }

    protected void setInitInput(int updateNum) {
        MathPreconditions.checkPositive("updateNum", updateNum);
        this.updateNum = updateNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        extraInfo++;
    }
}
