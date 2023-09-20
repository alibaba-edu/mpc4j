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

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * abstract private (distinct) set membership sender.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public abstract class AbstractPdsmSender extends AbstractTwoPartyPto implements PdsmSender {
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
     * inputs
     */
    protected byte[][][] inputArrays;

    public AbstractPdsmSender(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PdsmConfig config) {
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

    protected void setPtoInput(int l, byte[][][] inputArrays) {
        MathPreconditions.checkInRangeClosed("l", l, CommonConstants.STATS_BIT_LENGTH, maxL);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        MathPreconditions.checkInRangeClosed("inputArrays.num", inputArrays.length, 2, maxNum);
        num = inputArrays.length;
        MathPreconditions.checkPositiveInRangeClosed("d", inputArrays[0].length, maxD);
        d = inputArrays[0].length;
        // check all inputs are distinct
        long distinctCount = Arrays.stream(inputArrays)
            .peek(inputArray -> {
                // check point num
                MathPreconditions.checkEqual("d", "inputArray.length", d, inputArray.length);
                for (byte[] input : inputArray) {
                    Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(input, byteL, l));
                }
            })
            .flatMap(Arrays::stream)
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        MathPreconditions.checkEqual("distinct inputs", "d * num", distinctCount, (long) d * num);
        this.inputArrays = inputArrays;
    }
}
