package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

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
     * num
     */
    protected int num;
    /**
     * number of COTs
     */
    protected int cotNum;

    protected AbstractSspCotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, SspCotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(byte[] delta) {
        Preconditions.checkArgument(BlockUtils.valid(delta));
        this.delta = BytesUtils.clone(delta);
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        cotNum = SspCotFactory.getPrecomputeNum(config, num);
        extraInfo++;
    }

    protected void setPtoInput(int num, CotSenderOutput preSenderOutput) {
        setPtoInput(num);
        if (preSenderOutput != null) {
            Preconditions.checkArgument(BlockUtils.equals(delta, preSenderOutput.getDelta()));
            MathPreconditions.checkGreaterOrEqual("preNum", preSenderOutput.getNum(), cotNum);
        }
    }
}
