package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;

import java.math.BigInteger;

/**
 * abstract Zl core multiplication triple generator.
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public abstract class AbstractZlCoreMtgParty extends AbstractTwoPartyPto implements ZlCoreMtgParty {
    /**
     * config
     */
    protected final ZlCoreMtgConfig config;
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
    protected int byteL;
    /**
     * mask used for module operation
     */
    protected BigInteger mask;
    /**
     * max num
     */
    protected int maxNum;
    /**
     * num
     */
    protected int num;

    public AbstractZlCoreMtgParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlCoreMtgConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
        zl = config.getZl();
        l = zl.getL();
        byteL = zl.getByteL();
        mask = BigInteger.ONE.shiftLeft(l).subtract(BigInteger.ONE);
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositiveInRangeClosed("maxNum", maxNum, config.maxNum());
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        extraInfo++;
    }
}
