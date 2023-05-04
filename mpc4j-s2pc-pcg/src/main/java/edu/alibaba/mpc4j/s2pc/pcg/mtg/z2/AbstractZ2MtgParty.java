package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * 布尔三元组生成协议参与方。
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/02/07
 */
public abstract class AbstractZ2MtgParty extends AbstractTwoPartyPto implements Z2MtgParty {
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

    protected void setInitInput(int maxRoundNum, int updateNum) {
        MathPreconditions.checkPositiveInRangeClosed("maxRoundNum", maxRoundNum, updateNum);
        this.maxRoundNum = maxRoundNum;
        this.updateNum = updateNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxRoundNum);
        this.num = num;
        byteNum = CommonUtils.getByteLength(num);
        extraInfo += num;
    }
}
