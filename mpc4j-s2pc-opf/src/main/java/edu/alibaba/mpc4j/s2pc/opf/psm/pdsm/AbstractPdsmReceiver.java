package edu.alibaba.mpc4j.s2pc.opf.psm.pdsm;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.Arrays;

/**
 * abstract private (distinct) set membership receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public abstract class AbstractPdsmReceiver extends AbstractTwoPartyPto implements PdsmReceiver {
    /**
     * max num
     */
    private int maxNum;
    /**
     * max d
     */
    private int maxD;
    /**
     * max l
     */
    private int maxL;
    /**
     * num
     */
    protected int num;
    /**
     * point num
     */
    protected int d;
    /**
     * l
     */
    protected int l;
    /**
     * l in bytes
     */
    protected int byteL;
    /**
     * input array
     */
    protected byte[][] inputArray;

    public AbstractPdsmReceiver(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PdsmConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxL, int maxD, int maxNum) {
        MathPreconditions.checkGreaterOrEqual("maxL", maxL, CommonConstants.STATS_BIT_LENGTH);
        this.maxL = maxL;
        MathPreconditions.checkPositive("maxD", maxD);
        this.maxD = maxD;
        MathPreconditions.checkGreater("maxNum", maxNum, 1);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int l, int d, byte[][] inputArray) {
        MathPreconditions.checkGreaterOrEqual("l", l, CommonConstants.STATS_BIT_LENGTH);
        MathPreconditions.checkLessOrEqual("l", l, maxL);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        MathPreconditions.checkPositiveInRangeClosed("d", d, maxD);
        this.d = d;
        MathPreconditions.checkInRangeClosed("inputArrays.num", inputArray.length, 2, maxNum);
        num = inputArray.length;
        this.inputArray = Arrays.stream(inputArray)
            .peek(input -> Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(input, byteL, l)))
            .toArray(byte[][]::new);
    }
}
