package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
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

    protected void setInitInput(byte[] delta) {
        Preconditions.checkArgument(BlockUtils.valid(delta));
        this.delta = BlockUtils.clone(delta);
        initState();
    }

    protected void setPtoInput(int t, int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        MathPreconditions.checkPositiveInRangeClosed("t", t, num);
        this.t = t;
        extraInfo++;
    }

    protected void setPtoInput(int t, int num, CotSenderOutput preSenderOutput) {
        setPtoInput(t, num);
        if (preSenderOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preCotNum", preSenderOutput.getNum(), MspCotFactory.getPrecomputeNum(config, t, num)
            );
        }
    }
}
