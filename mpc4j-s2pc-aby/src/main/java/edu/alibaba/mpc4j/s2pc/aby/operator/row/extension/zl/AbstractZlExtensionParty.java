package edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

import java.util.stream.IntStream;

/**
 * Abstract Zl Value Extension Party.
 *
 * @author Liqiang Peng
 * @date 2024/5/29
 */
public abstract class AbstractZlExtensionParty extends AbstractTwoPartyPto implements ZlExtensionParty {
    /**
     * max num
     */
    protected int maxNum;
    /**
     * max input l
     */
    protected int maxInputL;
    /**
     * max output l
     */
    protected int maxOutputL;
    /**
     * num
     */
    protected int num;
    /**
     * input Zl instance
     */
    protected Zl inputZl;
    /**
     * output Zl instance
     */
    protected Zl outputZl;
    /**
     * xs
     */
    protected byte[][] xs;

    public AbstractZlExtensionParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlExtensionConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxInputL, int maxOutputL, int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        MathPreconditions.checkPositive("maxInputL", maxInputL);
        MathPreconditions.checkPositive("maxOutputL", maxOutputL);
        this.maxNum = maxNum;
        this.maxInputL = maxInputL;
        this.maxOutputL = maxOutputL;
        initState();
    }

    protected void setPtoInput(SquareZlVector xi, int outputL) {
        checkInitialized();
        MathPreconditions.checkLess("l", xi.getZl().getL(), outputL);
        MathPreconditions.checkPositiveInRangeClosed("num", xi.getNum(), maxNum);
        MathPreconditions.checkPositiveInRangeClosed("input l", xi.getZl().getL(), maxInputL);
        MathPreconditions.checkPositiveInRangeClosed("output l", outputL, maxOutputL);
        inputZl = xi.getZl();
        outputZl = ZlFactory.createInstance(envType, outputL);
        int byteL = inputZl.getByteL();
        num = xi.getNum();
        xs = new byte[num][byteL];
        IntStream.range(0, num).forEach(i ->
            xs[i] = BigIntegerUtils.nonNegBigIntegerToByteArray(xi.getZlVector().getElement(i), byteL)
        );
    }
}
