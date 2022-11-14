package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

/**
 * 核COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
public abstract class AbstractCoreCotReceiver extends AbstractSecureTwoPartyPto implements CoreCotReceiver {
    /**
     * 配置项
     */
    private final CoreCotConfig config;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 选择比特
     */
    protected boolean[] choices;
    /**
     * 数量
     */
    protected int num;

    protected AbstractCoreCotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, CoreCotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    @Override
    public CoreCotFactory.CoreCotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxNum) {
        assert maxNum > 0 : "max num must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        initialized = false;
    }

    protected void setPtoInput(boolean[] choices) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert choices.length > 0 && choices.length <= maxNum : "num must be in range (0, " + maxNum + "]: " + choices.length;
        // 拷贝一份
        this.choices = Arrays.copyOf(choices, choices.length);
        num = choices.length;
        extraInfo++;
    }
}
