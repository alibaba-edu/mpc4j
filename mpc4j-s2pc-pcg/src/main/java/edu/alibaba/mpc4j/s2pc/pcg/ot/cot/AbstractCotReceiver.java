package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * abstract COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public abstract class AbstractCotReceiver extends AbstractTwoPartyPto implements CotReceiver {
    /**
     * the config
     */
    protected final CotConfig config;
    /**
     * expect num
     */
    protected int expectNum;
    /**
     * choices
     */
    protected boolean[] choices;
    /**
     * num
     */
    protected int num;

    public AbstractCotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, CotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int expectNum) {
        MathPreconditions.checkPositive("expect_num", expectNum);
        this.expectNum = expectNum;
        initState();
    }

    protected void setPtoInput(boolean[] choices) {
        checkInitialized();
        MathPreconditions.checkPositive("num", choices.length);
        this.choices = choices;
        num = choices.length;
        extraInfo++;
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        extraInfo++;
    }
}
