package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * abstract no-choice COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/01/26
 */
public abstract class AbstractNcCotReceiver extends AbstractTwoPartyPto implements NcCotReceiver {
    /**
     * config
     */
    private final NcCotConfig config;
    /**
     * num
     */
    protected int num;

    protected AbstractNcCotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, NcCotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int num) {
        MathPreconditions.checkPositiveInRangeClosed("num", num, config.maxNum());
        this.num = num;
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
