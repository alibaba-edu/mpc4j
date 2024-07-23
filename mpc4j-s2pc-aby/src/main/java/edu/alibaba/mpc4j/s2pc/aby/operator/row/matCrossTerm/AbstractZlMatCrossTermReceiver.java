package edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Abstract Zl Matrix Cross Term Multiplication Receiver.
 *
 * @author Liqiang Peng
 * @date 2024/6/12
 */
public abstract class AbstractZlMatCrossTermReceiver extends AbstractTwoPartyPto implements ZlMatCrossTermParty {
    /**
     * max m
     */
    protected int maxM;
    /**
     * max n
     */
    protected int maxN;
    /**
     * max d1
     */
    protected int maxD1;
    /**
     * max d2
     */
    protected int maxD2;
    /**
     * max d3
     */
    protected int maxD3;
    /**
     * m
     */
    protected int m;
    /**
     * n
     */
    protected int n;
    /**
     * output Zl instance
     */
    protected Zl outputZl;
    /**
     * ys
     */
    protected byte[][] ys;
    /**
     * dimension d1
     */
    protected int d1;
    /**
     * dimension d2
     */
    protected int d2;
    /**
     * dimension d3
     */
    protected int d3;

    public AbstractZlMatCrossTermReceiver(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlMatCrossTermConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxM, int maxN, int maxD1, int maxD2, int maxD3) {
        MathPreconditions.checkPositive("maxM", maxM);
        MathPreconditions.checkPositive("maxN", maxN);
        MathPreconditions.checkPositive("maxD1", maxD1);
        MathPreconditions.checkPositive("maxD2", maxD2);
        MathPreconditions.checkPositive("maxD3", maxD3);
        this.maxM = maxM;
        this.maxN = maxN;
        this.maxD1 = maxD1;
        this.maxD2 = maxD2;
        this.maxD3 = maxD3;
        initState();
    }

    protected void setPtoInput(SquareZlVector y, int d1, int d2, int d3, int m, int n) {
        checkInitialized();
        Preconditions.checkArgument(y.isPlain());
        Preconditions.checkArgument(y.getZl().getL() == n);
        MathPreconditions.checkLessOrEqual("m <= n", m, y.getZl().getL());
        MathPreconditions.checkPositiveInRangeClosed("m", m, maxM);
        MathPreconditions.checkPositiveInRangeClosed("n", y.getZl().getL(), maxN);
        MathPreconditions.checkPositiveInRangeClosed("d1", d1, maxD1);
        MathPreconditions.checkPositiveInRangeClosed("d2", d2, maxD2);
        MathPreconditions.checkPositiveInRangeClosed("d3", d3, maxD3);
        this.d1 = d1;
        this.d2 = d2;
        this.d3 = d3;
        this.m = m;
        this.n = y.getZl().getL();
        MathPreconditions.checkEqual("input matrix size", "expect matrix size", y.getNum(), d2 * d3);
        MathPreconditions.checkPositive("d1", d1);
        outputZl = ZlFactory.createInstance(envType, m + n);
    }
}
