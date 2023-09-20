package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * abstract multi single-point COT sender.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public abstract class AbstractMspCotSender extends AbstractTwoPartyPto implements MspCotSender {
    /**
     * config
     */
    private final MspCotConfig config;
    /**
     * Δ
     */
    protected byte[] delta;
    /**
     * max num
     */
    private int maxNum;
    /**
     * max sparse num
     */
    private int maxT;
    /**
     * num
     */
    protected int num;
    /**
     * sparse num
     */
    protected int t;

    protected AbstractMspCotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, MspCotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(byte[] delta, int maxT, int maxNum) {
        MathPreconditions.checkEqual("Δ.length", "λ(B)", delta.length, CommonConstants.BLOCK_BYTE_LENGTH);
        this.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        MathPreconditions.checkPositiveInRangeClosed("maxT", maxT, maxNum);
        this.maxT = maxT;
        initState();
    }

    protected void setPtoInput(int t, int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        MathPreconditions.checkPositiveInRangeClosed("t", t, num);
        MathPreconditions.checkPositiveInRangeClosed("t", t, maxT);
        this.t = t;
        extraInfo++;
    }

    protected void setPtoInput(int t, int num, CotSenderOutput preSenderOutput) {
        setPtoInput(t, num);
        MathPreconditions.checkGreaterOrEqual(
            "preCotNum", preSenderOutput.getNum(), MspCotFactory.getPrecomputeNum(config, t, num)
        );
    }
}
