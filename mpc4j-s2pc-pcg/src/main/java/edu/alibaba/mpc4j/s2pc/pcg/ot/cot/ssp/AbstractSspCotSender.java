package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.util.Arrays;

/**
 * abstract SSP-COT sender.
 *
 * @author Weiran Liu
 * @date 2023/7/13
 */
public abstract class AbstractSspCotSender extends AbstractTwoPartyPto implements SspCotSender {
    /**
     * config
     */
    protected final SspCotConfig config;
    /**
     * Δ
     */
    protected byte[] delta;
    /**
     * max num
     */
    private int maxNum;
    /**
     * num
     */
    protected int num;

    protected AbstractSspCotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, SspCotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(byte[] delta, int maxNum) {
        MathPreconditions.checkEqual("Δ.length", "λ(B)", delta.length, CommonConstants.BLOCK_BYTE_LENGTH);
        this.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        extraInfo++;
    }

    protected void setPtoInput(int num, CotSenderOutput preSenderOutput) {
        setPtoInput(num);
        Preconditions.checkArgument(Arrays.equals(delta, preSenderOutput.getDelta()));
        MathPreconditions.checkGreaterOrEqual(
            "preNum", preSenderOutput.getNum(), SspCotFactory.getPrecomputeNum(config, num)
        );
    }
}
