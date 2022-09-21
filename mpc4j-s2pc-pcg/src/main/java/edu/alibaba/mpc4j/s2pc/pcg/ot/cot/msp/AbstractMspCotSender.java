package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * MSP-COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public abstract class AbstractMspCotSender extends AbstractSecureTwoPartyPto implements MspCotSender {
    /**
     * 配置项
     */
    private final MspCotConfig config;
    /**
     * 关联值Δ
     */
    protected byte[] delta;
    /**
     * 最大数量
     */
    protected int maxNum;
    /**
     * 最大稀疏点数量
     */
    protected int maxT;
    /**
     * 数量
     */
    protected int num;
    /**
     * 稀疏点数量
     */
    protected int t;

    protected AbstractMspCotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, MspCotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public MspCotFactory.MspCotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(byte[] delta, int maxT, int maxNum) {
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH;
        // 拷贝一份
        this.delta = BytesUtils.clone(delta);
        assert maxNum > 0 : "maxNum must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        assert maxT > 0 && maxT <= maxNum : "maxT must be in range (0, " + maxNum + "]: " + maxT;
        this.maxT = maxT;
        initialized = false;
    }

    protected void setPtoInput(int t, int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + maxNum + "]: " + num;
        this.num = num;
        assert t > 0 && t <= num && t <= maxT : "t must be in range (0, " + maxT + "]: " + t;
        this.t = t;
        extraInfo++;
    }

    protected void setPtoInput(int t, int num, CotSenderOutput preSenderOutput) {
        setPtoInput(t, num);
        assert preSenderOutput.getNum() >= MspCotFactory.getPrecomputeNum(config, t, num);
    }
}
