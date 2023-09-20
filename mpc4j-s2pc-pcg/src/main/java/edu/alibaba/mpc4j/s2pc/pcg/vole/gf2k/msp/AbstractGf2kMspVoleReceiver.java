package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

/**
 * abstract GF2K-MSP-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
public abstract class AbstractGf2kMspVoleReceiver extends AbstractTwoPartyPto implements Gf2kMspVoleReceiver {
    /**
     * config
     */
    private final Gf2kMspVoleConfig config;
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

    protected AbstractGf2kMspVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kMspVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
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

    protected void setPtoInput(int t, int num, Gf2kVoleReceiverOutput preReceiverOutput) {
        setPtoInput(t, num);
        MathPreconditions.checkGreaterOrEqual(
            "preNum", preReceiverOutput.getNum(), Gf2kMspVoleFactory.getPrecomputeNum(config, t, num)
        );
    }
}
