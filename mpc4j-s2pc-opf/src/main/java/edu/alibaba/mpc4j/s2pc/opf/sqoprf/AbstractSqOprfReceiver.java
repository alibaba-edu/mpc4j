package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * abstract single-query OPRF receiver.
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public abstract class AbstractSqOprfReceiver extends AbstractTwoPartyPto implements SqOprfReceiver {
    /**
     * max batch size
     */
    protected int maxBatchSize;
    /**
     * inputs
     */
    protected byte[][] inputs;
    /**
     * batch size
     */
    protected int batchSize;

    protected AbstractSqOprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, SqOprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(int maxBatchSize) {
        // single-query OPRF requires max batch size > 0
        MathPreconditions.checkPositive("maxBatchSize", maxBatchSize);
        this.maxBatchSize = maxBatchSize;
        initState();
    }

    protected void setPtoInput(byte[][] inputs) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("batchSize", inputs.length, maxBatchSize);
        this.inputs = inputs;
        batchSize = inputs.length;
        extraInfo++;
    }
}
