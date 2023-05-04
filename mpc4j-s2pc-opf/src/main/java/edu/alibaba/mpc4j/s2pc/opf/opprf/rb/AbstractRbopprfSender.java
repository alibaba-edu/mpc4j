package edu.alibaba.mpc4j.s2pc.opf.opprf.rb;

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
 * abstract Related-Batch OPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public abstract class AbstractRbopprfSender extends AbstractTwoPartyPto implements RbopprfSender {
    /**
     * max batch size
     */
    protected int maxBatchSize;
    /**
     * max point num
     */
    protected int maxPointNum;
    /**
     * l bit length
     */
    protected int l;
    /**
     * l byte length
     */
    protected int byteL;
    /**
     * batch size
     */
    protected int batchSize;
    /**
     * the number of target programmed points
     */
    protected int pointNum;
    /**
     * the batched input arrays.
     */
    protected byte[][][] inputArrays;
    /**
     * the batched target programmed arrays
     */
    protected byte[][][] targetArrays;


    protected AbstractRbopprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, RbopprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput(int maxBatchSize, int maxPointNum) {
        MathPreconditions.checkGreater("max batch size", maxBatchSize, 1);
        this.maxBatchSize = maxBatchSize;
        MathPreconditions.checkPositive("max point num", maxPointNum);
        this.maxPointNum = maxPointNum;
        initState();
    }

    protected void setPtoInput(int l, byte[][][] inputArrays, byte[][][] targetArrays) {
        checkInitialized();
        // check l
        MathPreconditions.checkGreaterOrEqual("l", l, CommonConstants.STATS_BIT_LENGTH);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        // check batch size
        batchSize = inputArrays.length;
        MathPreconditions.checkGreater("batch size", batchSize, 1);
        MathPreconditions.checkLessOrEqual("batch size", batchSize, maxBatchSize);
        MathPreconditions.checkEqual("target batch size", "batch size", targetArrays.length, batchSize);
        // check point num
        pointNum = Arrays.stream(inputArrays)
            .mapToInt(inputArray -> inputArray.length)
            .sum();
        MathPreconditions.checkPositive("point num", pointNum);
        MathPreconditions.checkLessOrEqual("point num", pointNum, maxPointNum);
        int targetNum = Arrays.stream(targetArrays)
            .mapToInt(targetArray -> targetArray.length)
            .sum();
        MathPreconditions.checkEqual("target num", "point num", targetNum, pointNum);
        // check input / target arrays
        IntStream.range(0, batchSize)
            .forEach(batchIndex -> {
                byte[][] inputArray = inputArrays[batchIndex];
                byte[][] targetArray = targetArrays[batchIndex];
                assert inputArray.length == targetArray.length;
                // all targets should have l-bit length
                for (byte[] target : targetArray) {
                    assert BytesUtils.isFixedReduceByteArray(target, byteL, l);
                }
            });
        // check all inputs are distinct
        long distinctCount = Arrays.stream(inputArrays)
            .flatMap(Arrays::stream)
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        MathPreconditions.checkEqual("distinct inputs", "point num", distinctCount, pointNum);
        this.inputArrays = inputArrays;
        this.targetArrays = targetArrays;
        extraInfo++;
    }
}
