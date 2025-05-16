package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * abstract BP-CDPPRF sender.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public abstract class AbstractBpCdpprfSender extends AbstractTwoPartyPto implements BpCdpprfSender {
    /**
     * config
     */
    private final BpCdpprfConfig config;
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

    protected AbstractBpCdpprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, BpCdpprfConfig config) {
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
        Preconditions.checkArgument(IntMath.isPowerOfTwo(eachNum));
        this.eachNum = eachNum;
        MathPreconditions.checkPositive("batchNum", batchNum);
        this.batchNum = batchNum;
        cotNum = BpCdpprfFactory.getPrecomputeNum(config, batchNum, eachNum);
        extraInfo++;
    }

    protected void setPtoInput(int batchNum, int eachNum, CotSenderOutput preSenderOutput) {
        setPtoInput(batchNum, eachNum);
        if (preSenderOutput != null) {
            // do not need to require equal Δ
            MathPreconditions.checkGreaterOrEqual(
                "preCotNum", preSenderOutput.getNum(), BpCdpprfFactory.getPrecomputeNum(config, batchNum, eachNum)
            );
        }
    }
}
