package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.util.Arrays;

/**
 * BSP-COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public abstract class AbstractBspCotSender extends AbstractTwoPartyPto implements BspCotSender {
    /**
     * 配置项
     */
    private final BspCotConfig config;
    /**
     * 关联值Δ
     */
    protected byte[] delta;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 最大批处理数量
     */
    protected int maxBatchNum;
    /**
     * 数量
     */
    protected int num;
    /**
     * 批处理数量
     */
    protected int batchNum;

    protected AbstractBspCotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, BspCotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(byte[] delta, int maxBatchNum, int maxNum) {
        MathPreconditions.checkEqual("Δ.length", "λ(B)", delta.length, CommonConstants.BLOCK_BYTE_LENGTH);
        // 拷贝一份
        this.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        MathPreconditions.checkPositive("maxBatchNum", maxBatchNum);
        this.maxBatchNum = maxBatchNum;
        initState();
    }

    protected void setPtoInput(int batchNum, int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        MathPreconditions.checkPositiveInRangeClosed("batchNum", batchNum, maxBatchNum);
        this.batchNum = batchNum;
        extraInfo++;
    }

    protected void setPtoInput(int batchNum, int num, CotSenderOutput preSenderOutput) {
        setPtoInput(batchNum, num);
        Preconditions.checkArgument(Arrays.equals(delta, preSenderOutput.getDelta()));
        MathPreconditions.checkGreaterOrEqual(
            "preCotNum", preSenderOutput.getNum(), BspCotFactory.getPrecomputeNum(config, batchNum, num)
        );
    }
}
