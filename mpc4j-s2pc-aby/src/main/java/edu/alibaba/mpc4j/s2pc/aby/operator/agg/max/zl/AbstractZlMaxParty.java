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
     * inputs
     */
    protected SquareZlVector[] inputs;

    public AbstractZlMaxParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlMaxConfig config) {
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
        Zl zl = xi.getZl();
        num = xi.getNum();
        inputs = Arrays.stream(xi.getZlVector().getElements())
            .map(e -> SquareZlVector.create(zl, new BigInteger[]{e}, false))
            .toArray(SquareZlVector[]::new);
    }
}
