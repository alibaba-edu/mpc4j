package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * abstract COT sender.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public abstract class AbstractCotSender extends AbstractTwoPartyPto implements CotSender {
    /**
     * the config
     */
    protected final CotConfig config;
    /**
     * Δ
     */
    protected byte[] delta;
    /**
     * max round num
     */
    protected int maxRoundNum;
    /**
     * update num
     */
    protected long updateNum;
    /**
     * num
     */
    protected int num;

    public AbstractCotSender(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, CotConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    protected void setInitInput(byte[] delta, int maxRoundNum, int updateNum) {
        MathPreconditions.checkEqual("Δ.length", "λ(B)", delta.length, CommonConstants.BLOCK_BYTE_LENGTH);
        this.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositiveInRangeClosed("maxRoundNum", maxRoundNum, updateNum);
        this.maxRoundNum = maxRoundNum;
        this.updateNum = updateNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxRoundNum);
        this.num = num;
        extraInfo++;
    }
}
