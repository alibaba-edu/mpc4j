package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * 核布尔三元组生成协议。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public abstract class AbstractZ2CoreMtgParty extends AbstractSecureTwoPartyPto implements Z2CoreMtgParty {
    /**
     * 配置项
     */
    protected final Z2CoreMtgConfig config;
    /**
     * 最大数量
     */
    protected int maxNum;
    /**
     * 数量
     */
    protected int num;
    /**
     * 字节数量
     */
    protected int byteNum;

    public AbstractZ2CoreMtgParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, Z2CoreMtgConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    @Override
    public Z2CoreMtgFactory.Z2CoreMtgType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxNum) {
        assert maxNum > 0 && maxNum <= config.maxAllowNum()
            : "maxNum must be in range (0, " + config.maxAllowNum() + "]: " + maxNum;
        this.maxNum = maxNum;
        initialized = false;
    }

    protected void setPtoInput(int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + maxNum + "]: " + num;
        this.num = num;
        byteNum = CommonUtils.getByteLength(num);
        extraInfo++;
    }
}
