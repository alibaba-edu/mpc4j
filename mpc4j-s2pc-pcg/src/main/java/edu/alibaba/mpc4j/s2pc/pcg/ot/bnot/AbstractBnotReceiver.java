package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BnotFactory.BnotType;

/**
 * 基础N选1-OT协议接收方。
 *
 * @author Hanwen Feng
 * @date 2022/07/20
 */
public abstract class AbstractBnotReceiver extends AbstractSecureTwoPartyPto implements BnotReceiver {
    /**
     * 配置项
     */
    private final BnotConfig config;
    /**
     * 选择值数组
     */
    protected int[] choices;
    /**
     * 最大选择值
     */
    protected int n;

    protected AbstractBnotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, BnotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    @Override
    public BnotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int n) {
        this.n = n;
        initialized = false;
    }

    protected void setPtoInput(int[] choices) {
        if (!initialized) {
            throw new IllegalStateException("Need init ...");
        }
        assert choices.length > 0;
        this.choices = choices;
        extraInfo++;
    }
}
