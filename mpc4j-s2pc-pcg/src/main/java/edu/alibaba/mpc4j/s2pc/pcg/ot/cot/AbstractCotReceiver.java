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
     * max round num
     */
    protected int maxRoundNum;
    /**
     * update num
     */
    protected long updateNum;
    /**
     * choices
     */
    protected boolean[] choices;
    /**
     * num
     */
    protected int num;

    public AbstractCotReceiver(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, CotConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    protected void setInitInput(int maxRoundNum, int updateNum) {
        MathPreconditions.checkPositiveInRangeClosed("maxRoundNum", maxRoundNum, updateNum);
        this.maxRoundNum = maxRoundNum;
        this.updateNum = updateNum;
        initState();
    }

    protected void setPtoInput(boolean[] choices) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", choices.length, maxRoundNum);
        this.choices = choices;
        num = choices.length;
        extraInfo++;
    }
}
