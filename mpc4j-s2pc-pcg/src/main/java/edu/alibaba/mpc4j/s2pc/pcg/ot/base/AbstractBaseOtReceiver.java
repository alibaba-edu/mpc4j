package edu.alibaba.mpc4j.s2pc.pcg.ot.base;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;

/**
 * 基础OT协议接收方。
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
public abstract class AbstractBaseOtReceiver extends AbstractTwoPartyPto implements BaseOtReceiver {
    /**
     * 密钥派生函数
     */
    protected final Kdf kdf;
    /**
     * 选择比特
     */
    protected boolean[] choices;
    /**
     * 数量
     */
    protected int num;

    protected AbstractBaseOtReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, BaseOtConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        kdf = KdfFactory.createInstance(envType);
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(boolean[] choices) {
        checkInitialized();
        MathPreconditions.checkPositive("num", choices.length);
        num = choices.length;
        this.choices = choices;
        extraInfo++;
    }
}
