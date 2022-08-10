package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
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
     * 最大数量对数
     */
    protected int maxH;
    /**
     * 最大批处理数量
     */
    protected int maxBatch;
    /**
     * 数量
     */
    protected int num;
    /**
     * 数量对数
     */
    protected int h;
    /**
     * 批处理数量
     */
    protected int batch;

    protected AbstractBspCotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, BspCotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public BspCotFactory.BspCotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(byte[] delta, int maxBatch, int maxNum) {
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH;
        // 拷贝一份
        this.delta = BytesUtils.clone(delta);
        assert maxNum > 0 : "maxNum must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        maxH = LongUtils.ceilLog2(maxNum);
        assert maxBatch > 0 : "maxBatch must be greater than 0:" + maxBatch;
        this.maxBatch = maxBatch;
        initialized = false;
    }

    protected void setPtoInput(int batch, int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + maxNum + "]: " + num;
        this.num = num;
        h = LongUtils.ceilLog2(num);
        assert batch > 0 && batch <= maxBatch : "batch must be in range (0, " + maxBatch + "]: " + batch;
        this.batch = batch;
        // 一次并行处理m个数据
        extraInfo += batch;
    }

    protected void setPtoInput(int batch, int num, CotSenderOutput preSenderOutput) {
        setPtoInput(batch, num);
        assert preSenderOutput.getNum() >= BspCotFactory.getPrecomputeNum(config, batch, num);
    }
}
