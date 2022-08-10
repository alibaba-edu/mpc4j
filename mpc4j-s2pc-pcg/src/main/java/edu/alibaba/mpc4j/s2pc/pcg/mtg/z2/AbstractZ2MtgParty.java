package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory.Z2MtgType;

/**
 * 布尔三元组生成协议参与方。
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/02/07
 */
public abstract class AbstractZ2MtgParty extends AbstractSecureTwoPartyPto implements Z2MtgParty {
    /**
     * 配置项
     */
    protected final Z2MtgConfig config;
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
    /**
     * 字节数量
     */
    protected int byteNum;

    public AbstractZ2MtgParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, Z2MtgConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    @Override
    public Z2MtgType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxRoundNum, int updateNum) {
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
        byteNum = CommonUtils.getByteLength(num);
        extraInfo += num;
    }
}
