package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

/**
 * l比特三元组生成协议参与方。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public abstract class AbstractZlMtgParty extends AbstractSecureTwoPartyPto implements ZlMtgParty {
    /**
     * 配置项
     */
    protected final ZlMtgConfig config;
    /**
     * 比特长度
     */
    protected int l;
    /**
     * 最大单轮数量
     */
    protected int maxRoundNum;
    /**
     * 更新数量
     */
    protected long updateNum;
    /**
     * 数量
     */
    protected int num;

    public AbstractZlMtgParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlMtgConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    @Override
    public ZlMtgFactory.ZlMtgType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int l, int maxRoundNum, int updateNum) {
        assert l > 0 : "l must be greater than 0: " + l;
        this.l = l;
        assert maxRoundNum > 0 && maxRoundNum <= config.maxBaseNum() :
            "maxRoundNum must be in range (0, " + config.maxBaseNum() + "]";
        this.maxRoundNum = maxRoundNum;
        assert updateNum >= maxRoundNum
            : "updateNum must be greater than or equal to " + maxRoundNum + ": " + updateNum;
        this.updateNum = updateNum;
        initialized = false;
    }

    protected void setPtoInput(int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxRoundNum : "num must be in range [0, " + maxRoundNum + "]:" + num;
        this.num = num;
        extraInfo += num;
    }
}
