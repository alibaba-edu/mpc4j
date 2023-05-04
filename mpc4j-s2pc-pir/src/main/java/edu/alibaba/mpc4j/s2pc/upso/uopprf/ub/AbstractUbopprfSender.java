package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * abstract unbalanced batched OPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public abstract class AbstractUbopprfSender extends AbstractTwoPartyPto implements UbopprfSender {
    /**
     * the input / output bit length
     */
    protected int l;
    /**
     * the input / output byte length
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
     * max batch point num
     */
    protected int maxBatchPointNum;
    /**
     * the batched input arrays.
     */
    protected byte[][][] inputArrays;
    /**
     * the batched target programmed arrays
     */
    protected byte[][][] targetArrays;

    protected AbstractUbopprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, UbopprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput(int l, byte[][][] inputArrays, byte[][][] targetArrays) {
        // check l
        MathPreconditions.checkGreaterOrEqual("l", l, CommonConstants.STATS_BIT_LENGTH);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        // check batch size
        batchSize = inputArrays.length;
        MathPreconditions.checkGreater("batch size", batchSize, 1);
        MathPreconditions.checkEqual("target batch size", "batch size", targetArrays.length, batchSize);
        // check point num
        pointNum = Arrays.stream(inputArrays)
            .mapToInt(inputArray -> inputArray.length)
            .sum();
        MathPreconditions.checkPositive("point num", pointNum);
        int targetNum = Arrays.stream(targetArrays)
            .mapToInt(targetArray -> targetArray.length)
            .sum();
        MathPreconditions.checkEqual("target num", "point num", targetNum, pointNum);
        maxBatchPointNum = MaxBinSizeUtils.expectMaxBinSize(pointNum, batchSize);
        // check input / target arrays
        IntStream.range(0, batchSize)
            .forEach(batchIndex -> {
                byte[][] inputArray = inputArrays[batchIndex];
                byte[][] targetArray = targetArrays[batchIndex];
                int batchPointNum = inputArray.length;
                assert targetArray.length == batchPointNum;
                // all inputs should be distinct
                assert Arrays.stream(inputArray).map(ByteBuffer::wrap).distinct().count() == batchPointNum;
                // all targets should have l-bit length
                for (byte[] target : targetArray) {
                    assert BytesUtils.isFixedReduceByteArray(target, byteL, l);
                }
            });
        this.inputArrays = inputArrays;
        this.targetArrays = targetArrays;
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
