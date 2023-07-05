package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

/**
 * abstract 1-out-of-n (with n = 2^l) sender.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public abstract class AbstractLnotSender extends AbstractTwoPartyPto implements LnotSender {
    /**
     * the config
     */
    protected final LnotConfig config;
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
     * update num
     */
    protected int updateNum;
    /**
     * num
     */
    protected int num;

    public AbstractLnotSender(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, LnotConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    protected void setInitInput(int l, int updateNum) {
        MathPreconditions.checkPositiveInRangeClosed("l", l, IntUtils.MAX_L);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        n = 1 << l;
        MathPreconditions.checkPositive("updateNum", updateNum);
        this.updateNum = updateNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        extraInfo++;
    }
}
