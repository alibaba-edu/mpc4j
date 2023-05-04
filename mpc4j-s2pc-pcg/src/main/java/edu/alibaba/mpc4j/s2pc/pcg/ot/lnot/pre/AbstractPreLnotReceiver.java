package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;

/**
 * abstract pre-compute 1-out-of-n (with n = 2^l) OT receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public abstract class AbstractPreLnotReceiver extends AbstractTwoPartyPto implements PreLnotReceiver {
    /**
     * pre-compute receiver output
     */
    protected LnotReceiverOutput preReceiverOutput;
    /**
     * the choice array
     */
    protected int[] choiceArray;
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

    protected AbstractPreLnotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, PreLnotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(LnotReceiverOutput preReceiverOutput, int[] choiceArray) {
        checkInitialized();
        MathPreconditions.checkPositive("num", preReceiverOutput.getNum());
        this.preReceiverOutput = preReceiverOutput;
        num = preReceiverOutput.getNum();
        l = preReceiverOutput.getL();
        n = preReceiverOutput.getN();
        MathPreconditions.checkEqual("choices.length", "num", choiceArray.length, num);
        this.choiceArray = choiceArray;
        extraInfo++;
    }
}
