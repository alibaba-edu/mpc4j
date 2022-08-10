package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

/**
 * 基础N选1-OT协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/07/22
 */
public abstract class AbstractBnotSender extends AbstractSecureTwoPartyPto implements BnotSender {
    /**
     * 配置项
     */
    private final BnotConfig config;
    /**
     * 最大选择值
     */
    protected int n;
    /**
     * 数量
     */
    protected int num;

    protected AbstractBnotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, BnotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public BnotFactory.BnotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int n) {
        this.n = n;
        initialized = false;
    }

    protected void setPtoInput(int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init ...");
        }
        assert num > 0;
        this.num = num;
        extraInfo++;
    }
}
