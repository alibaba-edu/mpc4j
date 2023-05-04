package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * MPOPRF发送方。
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public abstract class AbstractMpOprfSender extends AbstractOprfSender implements MpOprfSender {

    protected AbstractMpOprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, MpOprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    @Override
    protected void setInitInput(int maxBatchSize, int maxPrfNum) {
        // multi-point OPRF requires maxBatchSize > 0
        MathPreconditions.checkPositive("maxBatchSize", maxBatchSize);
        this.maxBatchSize = maxBatchSize;
        MathPreconditions.checkNonNegative("maxPrfNum", maxPrfNum);
        this.maxPrfNum = maxPrfNum;
        initState();
    }

    @Override
    protected void setPtoInput(int batchSize) {
        checkInitialized();
        // multi-point OPRF requires batchSize > 0
        MathPreconditions.checkPositiveInRangeClosed("batchSize", batchSize, maxBatchSize);
        this.batchSize = batchSize;
        extraInfo++;
    }
}
