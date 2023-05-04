package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * abstract pre-compute COT sender.
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public abstract class AbstractPreCotSender extends AbstractTwoPartyPto implements PreCotSender {
    /**
     * pre-compute sender output
     */
    protected CotSenderOutput preSenderOutput;
    /**
     * num
     */
    protected int num;

    protected AbstractPreCotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, PreCotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(CotSenderOutput preSenderOutput) {
        checkInitialized();
        MathPreconditions.checkPositive("num", preSenderOutput.getNum());
        this.preSenderOutput = preSenderOutput;
        num = preSenderOutput.getNum();
        extraInfo++;
    }
}
