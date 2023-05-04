package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * 核布尔三元组生成协议。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public abstract class AbstractZ2CoreMtgParty extends AbstractTwoPartyPto implements Z2CoreMtgParty {
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

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositiveInRangeClosed("maxNum", maxNum, config.maxNum());
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        byteNum = CommonUtils.getByteLength(num);
        extraInfo++;
    }
}
