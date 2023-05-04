package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * abstract pre-compute COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public abstract class AbstractPreCotReceiver extends AbstractTwoPartyPto implements PreCotReceiver {
    /**
     * pre-compute receiver output
     */
    protected CotReceiverOutput preReceiverOutput;
    /**
     * the choices
     */
    protected boolean[] choices;
    /**
     * num
     */
    protected int num;

    protected AbstractPreCotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, PreCotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(CotReceiverOutput preReceiverOutput, boolean[] choices) {
        checkInitialized();
        MathPreconditions.checkPositive("num", preReceiverOutput.getNum());
        this.preReceiverOutput = preReceiverOutput;
        num = preReceiverOutput.getNum();
        MathPreconditions.checkEqual("choices.length", "num", choices.length, num);
        this.choices = choices;
        extraInfo++;
    }
}
