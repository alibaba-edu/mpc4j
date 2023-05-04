package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;

import java.util.Arrays;

/**
 * 基础n选1-OT协议接收方。
 *
 * @author Hanwen Feng, Weiran Liu
 * @date 2022/07/20
 */
public abstract class AbstractBaseNotReceiver extends AbstractTwoPartyPto implements BaseNotReceiver {
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
        kdf = KdfFactory.createInstance(envType);
    }

    protected void setInitInput(int maxChoice) {
        MathPreconditions.checkGreater("n (max candidate choices)", maxChoice, 1);
        this.maxChoice = maxChoice;
        initState();
    }

    protected void setPtoInput(int[] choices) {
        checkInitialized();
        MathPreconditions.checkPositive("num", choices.length);
        num = choices.length;
        this.choices = Arrays.stream(choices)
            .peek(choice -> MathPreconditions.checkNonNegativeInRange("choice", choice, maxChoice))
            .toArray();
        extraInfo++;
    }
}
