package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * OPRF发送方。
 *
 * @author Weiran Liu
 * @date 2022/02/06
 */
public abstract class AbstractOprfSender extends AbstractTwoPartyPto implements OprfSender {
    /**
     * 最大批处理数量
     */
    protected int maxBatchSize;
    /**
     * scale num
     */
    protected int maxPrfNum;
    /**
     * 批处理数量
     */
    protected int batchSize;

    protected AbstractOprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, OprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput(int maxBatchSize, int maxPrfNum) {
        // standard OPRF requires maxBatchSize > 1
        MathPreconditions.checkGreater("maxBatchSize", maxBatchSize, 1);
        this.maxBatchSize = maxBatchSize;
        MathPreconditions.checkNonNegative("maxPrfNum", maxPrfNum);
        this.maxPrfNum = maxPrfNum;
        initState();
    }

    protected void setPtoInput(int batchSize) {
        checkInitialized();
        // standard OPRF requires batchSize > 1
        MathPreconditions.checkGreater("batchSize", batchSize, 1);
        MathPreconditions.checkLessOrEqual("batchSize", batchSize, maxBatchSize);
        this.batchSize = batchSize;
        extraInfo++;
    }
}
