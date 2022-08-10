package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotFactory.SspCotType;

/**
 * SSP-COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public abstract class AbstractSspCotSender extends AbstractSecureTwoPartyPto implements SspCotSender {
    /**
     * 配置项
     */
    private final SspCotConfig config;
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
     * 数量
     */
    protected int num;
    /**
     * 数量对数
     */
    protected int h;

    protected AbstractSspCotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, SspCotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public SspCotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(byte[] delta, int maxNum) {
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH;
        // 拷贝一份
        this.delta = BytesUtils.clone(delta);
        assert maxNum > 0 : "maxNum must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        maxH = LongUtils.ceilLog2(maxNum);
    }

    protected void setPtoInput(int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + maxNum + "]: " + num;
        this.num = num;
        h = LongUtils.ceilLog2(num);
        extraInfo++;
    }

    protected void setPtoInput(int num, CotSenderOutput preSenderOutput) {
        setPtoInput(num);
        assert preSenderOutput.getNum() >= SspCotFactory.getPrecomputeNum(config, num);
    }
}
