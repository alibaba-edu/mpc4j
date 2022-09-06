package edu.alibaba.mpc4j.s2pc.aby.bc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

/**
 * 布尔电路服务端。
 *
 * @author Weiran Liu
 * @date 2022/02/13
 */
public abstract class AbstractBcParty extends AbstractSecureTwoPartyPto implements BcParty {
    /**
     * 配置项
     */
    private final BcConfig config;
    /**
     * 最大单次运算数量
     */
    protected int maxRoundNum;
    /**
     * 更新数量
     */
    protected long maxUpdateNum;
    /**
     * 当前运算数量
     */
    protected int num;
    /**
     * AND门数量
     */
    protected long andGateNum;
    /**
     * XOR门数量
     */
    protected long xorGateNum;

    public AbstractBcParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, BcConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
        andGateNum = 0;
        xorGateNum = 0;
    }

    @Override
    public BcFactory.BcType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxRoundNum, int updateNum) {
        assert maxRoundNum > 0 && maxRoundNum <= config.maxBaseNum()
            : "maxRoundNum must be in range (0, " + config.maxBaseNum() + "]";
        this.maxRoundNum = maxRoundNum;
        assert updateNum >= maxRoundNum : "updateNum must be greater or equal to maxRoundNum";
        this.maxUpdateNum = updateNum;
        initialized = false;
    }

    protected void setAndInput(BcSquareVector x0, BcSquareVector y0) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert x0.bitLength() == y0.bitLength();
        assert x0.bitLength() <= maxRoundNum;
        // 只有当两组导线都为密文导线时，才需要增加门数量，这里不增加门数量
        num = x0.bitLength();
    }

    protected void setXorInput(BcSquareVector x0, BcSquareVector y0) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert x0.bitLength() == y0.bitLength();
        assert x0.bitLength() <= maxRoundNum;
        // 只有当两组导线都为密文导线时，才需要增加门数量，这里不增加门数量
        num = x0.bitLength();
    }
}
