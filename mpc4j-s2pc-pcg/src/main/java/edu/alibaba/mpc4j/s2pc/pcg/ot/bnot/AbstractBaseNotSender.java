package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;

/**
 * 基础n选1-OT协议发送方。
 *
 * @author Hanwen Feng, Weiran Liu
 * @date 2022/07/22
 */
public abstract class AbstractBaseNotSender extends AbstractSecureTwoPartyPto implements BaseNotSender {
    /**
     * 配置项
     */
    private final BaseNotConfig config;
    /**
     * 密钥派生函数
     */
    protected final Kdf kdf;
    /**
     * 最大选择值
     */
    protected int maxChoice;
    /**
     * 数量
     */
    protected int num;

    protected AbstractBaseNotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, BaseNotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
        kdf = KdfFactory.createInstance(envType);
    }

    @Override
    public BaseNotFactory.BaseNotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxChoice) {
        assert maxChoice > 1 : "n must be greater than 1: " + maxChoice;
        this.maxChoice = maxChoice;
        initialized = false;
    }

    protected void setPtoInput(int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init ...");
        }
        assert num > 0 : "num must be greater than 0: " + num;
        this.num = num;
        extraInfo++;
    }
}
