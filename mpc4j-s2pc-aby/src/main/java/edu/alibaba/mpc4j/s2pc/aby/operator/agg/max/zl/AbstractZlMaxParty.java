package edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Abstract Zl Max Party.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public abstract class AbstractZlMaxParty extends AbstractTwoPartyPto implements ZlMaxParty {
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
    /**
     * inputs
     */
    protected SquareZlVector[] inputs;

    public AbstractZlMaxParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlMaxConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }


    protected void setInitInput(int maxL, int maxNum) {
        MathPreconditions.checkPositive("maxL", maxL);
        this.maxL = maxL;
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(SquareZlVector xi) {
        num = xi.getNum();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        zl = xi.getZl();
        l = zl.getL();
        MathPreconditions.checkPositiveInRangeClosed("l", l, maxL);
        byteL = zl.getByteL();
        inputs = Arrays.stream(xi.getZlVector().getElements())
                .map(e -> SquareZlVector.create(zl, new BigInteger[]{e}, false)).toArray(SquareZlVector[]::new);
    }
}
