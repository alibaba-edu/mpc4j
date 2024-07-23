package edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;

/**
 * Abstract Zl Cross Term Multiplication Receiver.
 *
 * @author Liqiang Peng
 * @date 2024/6/5
 */
public abstract class AbstractZlCrossTermReceiver extends AbstractTwoPartyPto implements ZlCrossTermParty {
    /**
     * max m
     */
    protected int maxM;
    /**
     * max n
     */
    protected int maxN;
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
    protected byte[] ys;

    public AbstractZlCrossTermReceiver(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlCrossTermConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxM, int maxN) {
        MathPreconditions.checkPositiveInRangeClosed("maxM", maxM, Long.SIZE);
        MathPreconditions.checkPositiveInRangeClosed("maxN", maxN, Long.SIZE);
        this.maxM = maxM;
        this.maxN = maxN;
        initState();
    }

    protected void setPtoInput(BigInteger y, int m, int n) {
        checkInitialized();
        MathPreconditions.checkLessOrEqual("m <= n", m, n);
        MathPreconditions.checkPositiveInRangeClosed("m", m, maxM);
        MathPreconditions.checkPositiveInRangeClosed("n", n, maxN);
        Zl inputZl = ZlFactory.createInstance(envType, n);
        inputZl.validateElement(y);
        this.m = m;
        this.n = n;
        outputZl = ZlFactory.createInstance(envType, m + n);
        Preconditions.checkArgument(BigIntegerUtils.greaterOrEqual(BigInteger.ONE.shiftLeft(n), y));
    }
}
