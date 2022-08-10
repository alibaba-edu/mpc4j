package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public abstract class AbstractCotSender extends AbstractSecureTwoPartyPto implements CotSender {
    /**
     * 配置项
     */
    protected final CotConfig config;
    /**
     * 关联值Δ
     */
    protected byte[] delta;
    /**
     * 最大单轮数量
     */
    protected int maxRoundNum;
    /**
     * 更新数量
     */
    protected long updateNum;
    /**
     * 单次数量
     */
    protected int num;

    public AbstractCotSender(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, CotConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    @Override
    public CotFactory.CotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(byte[] delta, int maxRoundNum, int updateNum) {
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH
            : "Δ byte length must be " + CommonConstants.BLOCK_BYTE_LENGTH + ": " + delta.length;
        // 拷贝一份
        this.delta = BytesUtils.clone(delta);
        assert maxRoundNum > 0 && maxRoundNum <= config.maxBaseNum()
            : "maxRoundNum must be in range (0, " + config.maxBaseNum() + "]: " + maxRoundNum;
        this.maxRoundNum = maxRoundNum;
        assert updateNum >= maxRoundNum
            : "updateNum must be greater than or equal to " + maxRoundNum + "]: " + updateNum;
        this.updateNum = updateNum;
        initialized = false;
    }

    protected void setPtoInput(int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxRoundNum : "num must be in range [0, " + maxRoundNum + "]: " + num;
        this.num = num;
        extraInfo ++;
    }
}
