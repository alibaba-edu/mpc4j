package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * BSP-COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public abstract class AbstractBspCotSender extends AbstractSecureTwoPartyPto implements BspCotSender {
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

    @Override
    public BspCotFactory.BspCotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(byte[] delta, int maxBatchNum, int maxNum) {
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH;
        // 拷贝一份
        this.delta = BytesUtils.clone(delta);
        assert maxNum > 0 : "maxNum must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        assert maxBatchNum > 0 : "maxBatchNum must be greater than 0:" + maxBatchNum;
        this.maxBatchNum = maxBatchNum;
        initialized = false;
    }

    protected void setPtoInput(int batchNum, int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + maxNum + "]: " + num;
        this.num = num;
        assert batchNum > 0 && batchNum <= maxBatchNum : "batchNum must be in range (0, " + maxBatchNum + "]: " + batchNum;
        this.batchNum = batchNum;
        extraInfo++;
    }

    protected void setPtoInput(int batchNum, int num, CotSenderOutput preSenderOutput) {
        setPtoInput(batchNum, num);
        assert preSenderOutput.getNum() >= BspCotFactory.getPrecomputeNum(config, batchNum, num);
    }
}
