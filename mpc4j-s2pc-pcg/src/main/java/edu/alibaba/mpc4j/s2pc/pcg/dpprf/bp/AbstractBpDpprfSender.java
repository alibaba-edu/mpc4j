package edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * abstract batch-point DPPRF sender.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public abstract class AbstractBpDpprfSender extends AbstractTwoPartyPto implements BpDpprfSender {
    /**
     * config
     */
    protected final BpDpprfConfig config;
    /**
     * max α bound
     */
    protected int maxAlphaBound;
    /**
     * max α bit length
     */
    protected int maxH;
    /**
     * max batch num
     */
    protected int maxBatchNum;
    /**
     * α bound
     */
    protected int alphaBound;
    /**
     * α bit length
     */
    protected int h;
    /**
     * batch num
     */
    protected int batchNum;

    protected AbstractBpDpprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, BpDpprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(int maxBatchNum, int maxAlphaBound) {
        MathPreconditions.checkPositive("maxBatchNum", maxBatchNum);
        this.maxBatchNum = maxBatchNum;
        MathPreconditions.checkPositive("maxAlphaBound", maxAlphaBound);
        this.maxAlphaBound = maxAlphaBound;
        maxH = LongUtils.ceilLog2(maxAlphaBound, 1);
        initState();
    }

    protected void setPtoInput(int batchNum, int alphaBound) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("batchNum", batchNum, maxBatchNum);
        this.batchNum = batchNum;
        MathPreconditions.checkPositiveInRangeClosed("alphaBound", alphaBound, maxAlphaBound);
        this.alphaBound = alphaBound;
        h = LongUtils.ceilLog2(alphaBound, 1);
        extraInfo++;
    }

    protected void setPtoInput(int batchNum, int alphaBound, CotSenderOutput preSenderOutput) {
        setPtoInput(batchNum, alphaBound);
        MathPreconditions.checkGreaterOrEqual(
            "preCotNum", preSenderOutput.getNum(), BpDpprfFactory.getPrecomputeNum(config, batchNum, alphaBound)
        );
    }
}
