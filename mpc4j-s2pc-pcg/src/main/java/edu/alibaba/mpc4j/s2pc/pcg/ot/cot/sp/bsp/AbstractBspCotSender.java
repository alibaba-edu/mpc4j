package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.util.Arrays;

/**
 * abstract BSP-COT sender.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public abstract class AbstractBspCotSender extends AbstractTwoPartyPto implements BspCotSender {
    /**
     * config
     */
    private final BspCotConfig config;
    /**
     * Δ
     */
    protected byte[] delta;
    /**
     * each num
     */
    protected int eachNum;
    /**
     * batch num
     */
    protected int batchNum;
    /**
     * number of COTs
     */
    protected int cotNum;

    protected AbstractBspCotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, BspCotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(byte[] delta) {
        Preconditions.checkArgument(BlockUtils.valid(delta));
        this.delta = BlockUtils.clone(delta);
        initState();
    }

    protected void setPtoInput(int batchNum, int eachNum) {
        checkInitialized();
        MathPreconditions.checkPositive("eachNum", eachNum);
        this.eachNum = eachNum;
        MathPreconditions.checkPositive("batchNum", batchNum);
        this.batchNum = batchNum;
        cotNum = BspCotFactory.getPrecomputeNum(config, batchNum, eachNum);
        extraInfo++;
    }

    protected void setPtoInput(int batchNum, int eachNum, CotSenderOutput preSenderOutput) {
        setPtoInput(batchNum, eachNum);
        if (preSenderOutput != null) {
            Preconditions.checkArgument(Arrays.equals(delta, preSenderOutput.getDelta()));
            MathPreconditions.checkGreaterOrEqual("preCotNum", preSenderOutput.getNum(), cotNum);
        }
    }
}
