package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * MPOPRF接收方。
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public abstract class AbstractMpOprfReceiver extends AbstractOprfReceiver implements MpOprfReceiver {

    protected AbstractMpOprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, MpOprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
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
    protected void setPtoInput(byte[][] inputs) {
        checkInitialized();
        // standard OPRF requires batchSize > 0
        MathPreconditions.checkPositiveInRangeClosed("batchSize", inputs.length, maxBatchSize);
        this.inputs = inputs;
        batchSize = inputs.length;
        extraInfo++;
    }
}
