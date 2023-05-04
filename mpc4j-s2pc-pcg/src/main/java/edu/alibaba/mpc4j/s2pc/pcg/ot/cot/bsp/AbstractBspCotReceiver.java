package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * BSP-COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public abstract class AbstractBspCotReceiver extends AbstractTwoPartyPto implements BspCotReceiver {
    /**
     * 配置项
     */
    private final BspCotConfig config;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 最大批处理数量
     */
    private int maxBatchNum;
    /**
     * 单点索引值数组
     */
    protected int[] alphaArray;
    /**
     * 数量
     */
    protected int num;
    /**
     * 批处理数量
     */
    protected int batchNum;

    protected AbstractBspCotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, BspCotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int maxBatchNum, int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        MathPreconditions.checkPositive("maxBatchNum", maxBatchNum);
        this.maxBatchNum = maxBatchNum;
        initState();
    }

    protected void setPtoInput(int[] alphaArray, int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        batchNum = alphaArray.length;
        MathPreconditions.checkPositiveInRangeClosed("batchNum", batchNum, maxBatchNum);
        this.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> MathPreconditions.checkNonNegativeInRange("α", alpha, num))
            .toArray();
        extraInfo++;
    }

    protected void setPtoInput(int[] alphaArray, int num, CotReceiverOutput preReceiverOutput) {
        setPtoInput(alphaArray, num);
        MathPreconditions.checkGreaterOrEqual(
            "preCotNum", preReceiverOutput.getNum(), BspCotFactory.getPrecomputeNum(config, batchNum, num)
        );
    }
}
