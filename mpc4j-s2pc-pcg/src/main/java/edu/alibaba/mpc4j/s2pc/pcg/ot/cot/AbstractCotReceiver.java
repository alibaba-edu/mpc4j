package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

import java.util.Arrays;

/**
 * COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public abstract class AbstractCotReceiver extends AbstractSecureTwoPartyPto implements CotReceiver {
    /**
     * 配置项
     */
    protected final CotConfig config;
    /**
     * 最大单轮数量
     */
    protected int maxRoundNum;
    /**
     * 更新数量
     */
    protected long updateNum;
    /**
     * 选择比特
     */
    protected boolean[] choices;
    /**
     * 选择比特数量
     */
    protected int num;

    public AbstractCotReceiver(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, CotConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    @Override
    public CotFactory.CotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxRoundNum, int updateNum) {
        assert maxRoundNum > 0 && maxRoundNum <= config.maxBaseNum()
            : "maxRoundNum must be in range (0, " + config.maxBaseNum() + "]: " + maxRoundNum;
        this.maxRoundNum = maxRoundNum;
        assert updateNum >= maxRoundNum
            : "updateNum must be greater than or equal to " + maxRoundNum + "]: " + updateNum;
        this.updateNum = updateNum;
        initialized = false;
    }

    protected void setPtoInput(boolean[] choices) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert choices.length > 0 && choices.length <= maxRoundNum
            : "num must be in range (0, " + maxRoundNum + "]: " + choices.length;
        // 拷贝一份
        this.choices = Arrays.copyOf(choices, choices.length);
        num = choices.length;
        extraInfo++;
    }
}
