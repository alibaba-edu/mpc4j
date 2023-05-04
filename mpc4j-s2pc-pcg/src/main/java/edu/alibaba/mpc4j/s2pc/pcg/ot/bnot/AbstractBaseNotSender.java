package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;

/**
 * 基础n选1-OT协议发送方。
 *
 * @author Hanwen Feng, Weiran Liu
 * @date 2022/07/22
 */
public abstract class AbstractBaseNotSender extends AbstractTwoPartyPto implements BaseNotSender {
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
        kdf = KdfFactory.createInstance(envType);
    }

    protected void setInitInput(int maxChoice) {
        MathPreconditions.checkGreater("n (max candidate choices)", maxChoice, 1);
        this.maxChoice = maxChoice;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        extraInfo++;
    }
}
