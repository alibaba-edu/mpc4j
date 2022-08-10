package edu.alibaba.mpc4j.s2pc.pso.oprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

/**
 * OPRF接收方。
 *
 * @author Weiran Liu
 * @date 2022/02/06
 */
public abstract class AbstractOprfReceiver extends AbstractSecureTwoPartyPto implements OprfReceiver {
    /**
     * 配置项
     */
    private final OprfConfig config;
    /**
     * 最大批处理数量
     */
    private long maxBatchSize;
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
        this.config = config;
    }

    @Override
    public OprfFactory.OprfType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxBatchSize) {
        assert maxBatchSize > 0 : "MaxBatchSize must be greater than 0:" + maxBatchSize;
        this.maxBatchSize = maxBatchSize;
        initialized = false;
    }

    protected void setBatchInitInput(int maxBatchSize) {
        assert maxBatchSize > 1 : "MaxBatchSize must be greater than 1:" + maxBatchSize;
        this.maxBatchSize = maxBatchSize;
        initialized = false;
    }

    protected void setPtoInput(byte[][] inputs) {
        assert inputs.length > 0 && inputs.length <= maxBatchSize
            : "BatchSize must be in range [1, " + maxBatchSize + "]: " + inputs.length;
        this.inputs = inputs;
        batchSize = inputs.length;
        extraInfo++;
    }

    protected void setBatchPtoInput(byte[][] inputs) {
        assert inputs.length > 1 && inputs.length <= maxBatchSize
            : "BatchSize must be in range (1, " + maxBatchSize + "]: " + inputs.length;
        this.inputs = inputs;
        batchSize = inputs.length;
        extraInfo++;
    }
}
