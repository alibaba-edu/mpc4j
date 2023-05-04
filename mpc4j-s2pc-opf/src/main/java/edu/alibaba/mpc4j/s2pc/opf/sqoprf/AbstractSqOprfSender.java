package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * abstract single-query OPRF sender.
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public abstract class AbstractSqOprfSender extends AbstractTwoPartyPto implements SqOprfSender {
    /**
     * match batch size
     */
    protected int maxBatchSize;
    /**
     * batch size
     */
    protected int batchSize;

    protected AbstractSqOprfSender(PtoDesc ptoDesc, Rpc rpc, Party otherParty, SqOprfConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    protected void setInitInput(int maxBatchSize) {
        // single-query OPRF requires max batch size > 0
        MathPreconditions.checkPositive("maxBatchSize", maxBatchSize);
        this.maxBatchSize = maxBatchSize;
        initState();
    }

    protected void setPtoInput(int batchSize) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("batchSize", batchSize, maxBatchSize);
        this.batchSize = batchSize;
        extraInfo++;
    }
}
