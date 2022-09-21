package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BaseNotFactory.BaseNotType;

import java.util.Arrays;

/**
 * 基础n选1-OT协议接收方。
 *
 * @author Hanwen Feng, Weiran Liu
 * @date 2022/07/20
 */
public abstract class AbstractBaseNotReceiver extends AbstractSecureTwoPartyPto implements BaseNotReceiver {
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
     * 选择值数组
     */
    protected int[] choices;
    /**
     * 数量
     */
    protected int num;

    protected AbstractBaseNotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, BaseNotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
        kdf = KdfFactory.createInstance(envType);
    }

    @Override
    public BaseNotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxChoice) {
        assert maxChoice > 1 : "n must be greater than 1: " + maxChoice;
        this.maxChoice = maxChoice;
        initialized = false;
    }

    protected void setPtoInput(int[] choices) {
        if (!initialized) {
            throw new IllegalStateException("Need init ...");
        }
        assert choices.length > 0 : "num must be greater than 0: " + choices.length;
        num = choices.length;
        this.choices = Arrays.stream(choices)
            .peek(choice -> {
                assert choice >= 0 && choice < maxChoice : "choice must be in range [0, " + maxChoice + "): " + choice;
            })
            .toArray();
        extraInfo++;
    }
}
