package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;

/**
 * abstract pre-compute 1-out-of-n (with n = 2^l) OT sender.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public abstract class AbstractPreLnotSender extends AbstractTwoPartyPto implements PreLnotSender {
    /**
     * pre-compute sender output
     */
    protected LnotSenderOutput preSenderOutput;
    /**
     * num
     */
    protected int num;
    /**
     * choice bit length
     */
    protected int l;
    /**
     * the maximal choice
     */
    protected int n;

    protected AbstractPreLnotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, PreLnotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(LnotSenderOutput preSenderOutput) {
        checkInitialized();
        MathPreconditions.checkPositive("num", preSenderOutput.getNum());
        this.preSenderOutput = preSenderOutput;
        num = preSenderOutput.getNum();
        l = preSenderOutput.getL();
        n = preSenderOutput.getN();
        extraInfo++;
    }
}
