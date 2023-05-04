package edu.alibaba.mpc4j.s2pc.pcg.ot.base;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;

/**
 * 基础OT协议发送方。
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
public abstract class AbstractBaseOtSender extends AbstractTwoPartyPto implements BaseOtSender {
    /**
     * 密钥派生函数
     */
    protected final Kdf kdf;
    /**
     * 数量
     */
    protected int num;

    protected AbstractBaseOtSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, BaseOtConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        kdf = KdfFactory.createInstance(envType);
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        extraInfo++;
    }
}
