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
 * Abstract Zl Matrix Cross Term Multiplication Sender.
 *
 * @author Liqiang Peng
 * @date 2024/6/5
 */
public abstract class AbstractZlMatCrossTermSender extends AbstractTwoPartyPto implements ZlMatCrossTermParty {
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
     * xs
     */
    protected boolean[][] xs;
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

    public AbstractZlMatCrossTermSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, ZlMatCrossTermConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
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

    protected void setPtoInput(SquareZlVector x, int d1, int d2, int d3, int m, int n) {
        checkInitialized();
        Preconditions.checkArgument(x.isPlain());
        Preconditions.checkArgument(x.getZl().getL() == m);
        MathPreconditions.checkLessOrEqual("m <= n", x.getZl().getL(), n);
        MathPreconditions.checkPositiveInRangeClosed("m", x.getZl().getL(), maxM);
        MathPreconditions.checkPositiveInRangeClosed("n", n, maxN);
        MathPreconditions.checkPositiveInRangeClosed("d1", d1, maxD1);
        MathPreconditions.checkPositiveInRangeClosed("d2", d2, maxD2);
        MathPreconditions.checkPositiveInRangeClosed("d3", d3, maxD3);
        this.d1 = d1;
        this.d2 = d2;
        this.d3 = d3;
        this.m = x.getZl().getL();
        this.n = n;
        MathPreconditions.checkEqual("input matrix size", "expect matrix size", x.getNum(), d1 * d2);
        MathPreconditions.checkPositive("d3", d3);
        outputZl = ZlFactory.createInstance(envType, m + n);
    }
}
