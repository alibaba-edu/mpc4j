package edu.alibaba.mpc4j.s2pc.opf.psm.pesm;

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
 * abstract private (equal) set membership sender.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public abstract class AbstractPesmSender extends AbstractTwoPartyPto implements PesmSender {
    /**
     * max num
     */
    private int maxNum;
    /**
     * max set size
     */
    private int maxD;
    /**
     * max l
     */
    private int maxL;
    /**
     * point num
     */
    protected int d;
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
     * inputs
     */
    protected byte[][][] inputArrays;

    public AbstractPesmSender(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PesmConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxL, int maxD, int maxNum) {
        MathPreconditions.checkGreaterOrEqual("maxL", maxL, CommonConstants.STATS_BIT_LENGTH);
        this.maxL = maxL;
        MathPreconditions.checkPositive("maxD", maxD);
        this.maxD = maxD;
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int l, byte[][][] inputArrays) {
        MathPreconditions.checkGreaterOrEqual("l", l, CommonConstants.STATS_BIT_LENGTH);
        MathPreconditions.checkLessOrEqual("l", l, maxL);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        MathPreconditions.checkPositiveInRangeClosed("inputArrays.num", inputArrays.length, maxNum);
        num = inputArrays.length;
        d = inputArrays[0].length;
        MathPreconditions.checkPositiveInRangeClosed("d", d, maxD);
        this.inputArrays = Arrays.stream(inputArrays)
            .peek(inputArray -> {
                // check point num
                MathPreconditions.checkEqual("d", "inputArray.length", d, inputArray.length);
                for (byte[] input : inputArray) {
                    Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(input, byteL, l));
                }
            })
            .toArray(byte[][][]::new);
    }
}
