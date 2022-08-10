package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * Z2-核VOLE协议接收方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
public abstract class AbstractZ2CoreVoleReceiver extends AbstractSecureTwoPartyPto implements Z2CoreVoleReceiver {
    /**
     * 配置项
     */
    private final Z2CoreVoleConfig config;
    /**
     * 关联值Δ
     */
    protected boolean delta;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 数量
     */
    protected int num;
    /**
     * 字节数量
     */
    protected int byteNum;

    protected AbstractZ2CoreVoleReceiver(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Z2CoreVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public Z2CoreVoleFactory.Z2CoreVoleType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(boolean delta, int maxNum) {
        this.delta = delta;
        assert maxNum > 0: "maxNum must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        initialized = false;
    }

    protected void setPtoInput(int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxNum;
        this.num = num;
        byteNum = CommonUtils.getByteLength(num);
        extraInfo++;
    }
}
