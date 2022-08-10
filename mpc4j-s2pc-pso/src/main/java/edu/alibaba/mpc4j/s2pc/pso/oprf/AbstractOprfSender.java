package edu.alibaba.mpc4j.s2pc.pso.oprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

/**
 * OPRF发送方。
 *
 * @author Weiran Liu
 * @date 2022/02/06
 */
public abstract class AbstractOprfSender extends AbstractSecureTwoPartyPto implements OprfSender {
    /**
     * 配置项
     */
    private final OprfConfig config;
    /**
     * 最大批处理数量
     */
    protected long maxBatchSize;
    /**
     * 批处理数量
     */
    protected int batchSize;

    protected AbstractOprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, OprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
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

    protected void setPtoInput(int batchSize) {
        assert batchSize > 0 && batchSize <= maxBatchSize
            : "BatchSize must be in range [1, " + maxBatchSize + "]: " + batchSize;
        this.batchSize = batchSize;
        extraInfo++;
    }

    protected void setBatchPtoInput(int batchSize) {
        assert batchSize > 1 && batchSize <= maxBatchSize
            : "BatchSize must be in range (1, " + maxBatchSize + "]: " + batchSize;
        this.batchSize = batchSize;
        extraInfo++;
    }
}
