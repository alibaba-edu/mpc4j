package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * OPRF接收方。
 *
 * @author Weiran Liu
 * @date 2022/02/06
 */
public abstract class AbstractOprfReceiver extends AbstractTwoPartyPto implements OprfReceiver {
    /**
     * 最大批处理数量
     */
    protected int maxBatchSize;
    /**
     * scale num
     */
    protected int maxPrfNum;
    /**
     * 输入数组
     */
    protected byte[][] inputs;
    /**
     * 批处理数量
     */
    protected int batchSize;

    protected AbstractOprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, OprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(int maxBatchSize, int maxPrfNum) {
        // standard OPRF requires maxBatchSize > 1
        MathPreconditions.checkGreater("maxBatchSize", maxBatchSize, 1);
        this.maxBatchSize = maxBatchSize;
        MathPreconditions.checkNonNegative("maxPrfNum", maxPrfNum);
        this.maxPrfNum = maxPrfNum;
        initState();
    }

    protected void setPtoInput(byte[][] inputs) {
        checkInitialized();
        // standard OPRF requires batchSize > 1
        MathPreconditions.checkGreater("batchSize", inputs.length, 1);
        MathPreconditions.checkLessOrEqual("batchSize", inputs.length, maxBatchSize);
        this.inputs = inputs;
        batchSize = inputs.length;
        extraInfo++;
    }
}
