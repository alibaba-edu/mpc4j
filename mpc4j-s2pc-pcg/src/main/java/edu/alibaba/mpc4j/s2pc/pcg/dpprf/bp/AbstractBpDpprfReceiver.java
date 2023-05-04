package edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * abstract batch-point DPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public abstract class AbstractBpDpprfReceiver extends AbstractTwoPartyPto implements BpDpprfReceiver {
    /**
     * config
     */
    protected final BpDpprfConfig config;
    /**
     * max α upper bound
     */
    protected int maxAlphaBound;
    /**
     * max α bit length
     */
    protected int maxH;
    /**
     * max batch num
     */
    private int maxBatchNum;
    /**
     * α upper bound
     */
    protected int alphaBound;
    /**
     * α array
     */
    protected int[] alphaArray;
    /**
     * α binary arrays
     */
    protected boolean[][] alphaBinaryArray;
    /**
     * negative α binary arrays
     */
    protected boolean[][] notAlphaBinaryArray;
    /**
     * α bit length
     */
    protected int h;
    /**
     * batch num
     */
    protected int batchNum;

    protected AbstractBpDpprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, BpDpprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int maxBatchNum, int maxAlphaBound) {
        MathPreconditions.checkPositive("maxBatchNum", maxBatchNum);
        this.maxBatchNum = maxBatchNum;
        MathPreconditions.checkPositive("maxAlphaBound", maxAlphaBound);
        this.maxAlphaBound = maxAlphaBound;
        maxH = LongUtils.ceilLog2(maxAlphaBound, 1);
        initState();
    }

    protected void setPtoInput(int[] alphaArray, int alphaBound) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("alphaBound", alphaBound, maxAlphaBound);
        this.alphaBound = alphaBound;
        h = LongUtils.ceilLog2(alphaBound, 1);
        batchNum = alphaArray.length;
        MathPreconditions.checkPositiveInRangeClosed("batchNum", batchNum, maxBatchNum);
        this.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> MathPreconditions.checkNonNegativeInRange("alpha", alpha, alphaBound))
            .toArray();
        int offset = Integer.SIZE - h;
        alphaBinaryArray = new boolean[batchNum][h];
        notAlphaBinaryArray = new boolean[batchNum][h];
        IntStream.range(0, batchNum).forEach(index -> {
            int alpha = alphaArray[index];
            // 将α展开成二进制
            byte[] alphaBytes = IntUtils.intToByteArray(alpha);
            IntStream.range(0, h).forEach(i -> {
                alphaBinaryArray[index][i] = BinaryUtils.getBoolean(alphaBytes, offset + i);
                notAlphaBinaryArray[index][i] = !alphaBinaryArray[index][i];
            });
        });
        extraInfo++;
    }

    protected void setPtoInput(int[] alphaArray, int alphaBound, CotReceiverOutput preReceiverOutput) {
        setPtoInput(alphaArray, alphaBound);
        MathPreconditions.checkGreaterOrEqual(
            "preCotNum", preReceiverOutput.getNum(), BpDpprfFactory.getPrecomputeNum(config, batchNum, alphaBound)
        );
    }
}
