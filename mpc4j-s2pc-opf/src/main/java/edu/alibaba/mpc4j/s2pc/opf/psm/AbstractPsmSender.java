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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * abstract private set membership sender.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public abstract class AbstractPsmSender extends AbstractTwoPartyPto implements PsmSender {
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
     * inputs
     */
    protected byte[][][] inputArrays;

    public AbstractPsmSender(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PsmConfig config) {
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

    protected void setPtoInput(int l, byte[][][] inputArrays) {
        MathPreconditions.checkGreaterOrEqual("l", l, CommonConstants.STATS_BIT_LENGTH);
        MathPreconditions.checkLessOrEqual("l", l, maxL);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        MathPreconditions.checkGreater("inputArrays.num", inputArrays.length, 1);
        MathPreconditions.checkPositiveInRangeClosed("inputArrays.num", inputArrays.length, maxNum);
        num = inputArrays.length;
        // check all inputs are distinct
        long distinctCount = Arrays.stream(inputArrays)
            .flatMap(Arrays::stream)
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        MathPreconditions.checkEqual("distinct inputs", "d * num", distinctCount, (long) d * num);
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
