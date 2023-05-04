package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * l比特三元组生成协议参与方。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public abstract class AbstractZlMtgParty extends AbstractTwoPartyPto implements ZlMtgParty {
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

    protected void setInitInput(int l, int maxRoundNum, int updateNum) {
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        MathPreconditions.checkPositiveInRangeClosed("maxRoundNum", maxRoundNum, config.maxBaseNum());
        this.maxRoundNum = maxRoundNum;
        MathPreconditions.checkGreaterOrEqual("updateNum", updateNum, maxRoundNum);
        this.updateNum = updateNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxRoundNum);
        this.num = num;
        extraInfo += num;
    }
}
