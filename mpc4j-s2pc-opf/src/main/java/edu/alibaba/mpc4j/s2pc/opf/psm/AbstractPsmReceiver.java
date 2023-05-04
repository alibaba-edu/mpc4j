package edu.alibaba.mpc4j.s2pc.opf.psm;

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
 * abstract private set membership receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public abstract class AbstractPsmReceiver extends AbstractTwoPartyPto implements PsmReceiver {
    /**
     * max num
     */
    protected int maxNum;
    /**
     * point num
     */
    protected int d;
    /**
     * max l
     */
    protected int maxL;
    /**
     * num
     */
    protected int num;
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

    public AbstractPsmReceiver(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PsmConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxL, int d, int maxNum) {
        MathPreconditions.checkGreaterOrEqual("maxL", maxL, CommonConstants.STATS_BIT_LENGTH);
        this.maxL = maxL;
        MathPreconditions.checkPositive("d", d);
        this.d = d;
        MathPreconditions.checkGreater("maxNum", maxNum, 1);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int l, byte[][] inputArray) {
        MathPreconditions.checkGreaterOrEqual("l", l, CommonConstants.STATS_BIT_LENGTH);
        MathPreconditions.checkLessOrEqual("l", l, maxL);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        MathPreconditions.checkGreater("inputArrays.num", inputArray.length, 1);
        MathPreconditions.checkPositiveInRangeClosed("inputArray.num", inputArray.length, maxNum);
        num = inputArray.length;
        this.inputArray = Arrays.stream(inputArray)
            .peek(input -> Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(input, byteL, l)))
            .toArray(byte[][]::new);
    }
}
