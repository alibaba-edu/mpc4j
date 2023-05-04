package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

/**
 * abstract no-choice 1-out-of-n (with n = 2^l) OT receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public abstract class AbstractNcLnotReceiver extends AbstractTwoPartyPto implements NcLnotReceiver {
    /**
     * config
     */
    private final NcLnotConfig config;
    /**
     * choice bit length
     */
    protected int l;
    /**
     * choice byte length
     */
    protected int byteL;
    /**
     * maximal choice
     */
    protected int n;
    /**
     * num
     */
    protected int num;

    protected AbstractNcLnotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, NcLnotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int l, int num) {
        MathPreconditions.checkPositiveInRangeClosed("l", l, IntUtils.MAX_L);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        n = 1 << l;
        MathPreconditions.checkPositiveInRangeClosed("num", num, config.maxNum());
        this.num = num;
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
