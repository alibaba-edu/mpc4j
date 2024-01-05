package edu.alibaba.mpc4j.work.vectoried;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirConfig;
import edu.alibaba.mpc4j.work.BatchPirConfig;
import edu.alibaba.mpc4j.work.BatchPirFactory;

/**
 * Vectorized Batch PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class VectorizedBatchPirConfig extends AbstractMultiPartyPtoConfig implements BatchPirConfig {
    /**
     * vectorized batch PIR config
     */
    private final Mr23BatchIndexPirConfig vectorizedBatchPirConfig;

    public VectorizedBatchPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.vectorizedBatchPirConfig);
        vectorizedBatchPirConfig = builder.vectorizedBatchPirConfig;
    }

    @Override
    public BatchPirFactory.BatchIndexPirType getPtoType() {
        return BatchPirFactory.BatchIndexPirType.VECTORIZED_BATCH_PIR;
    }

    public Mr23BatchIndexPirConfig getVectorizedBatchPirConfig() {
        return vectorizedBatchPirConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<VectorizedBatchPirConfig> {
        /**
         * vectorized batch PIR config
         */
        private Mr23BatchIndexPirConfig vectorizedBatchPirConfig;

        public Builder() {
            vectorizedBatchPirConfig = new Mr23BatchIndexPirConfig.Builder().build();
        }

        public Builder setVectorizedBatchPirConfig(Mr23BatchIndexPirConfig config) {
            this.vectorizedBatchPirConfig = config;
            return this;
        }

        @Override
        public VectorizedBatchPirConfig build() {
            return new VectorizedBatchPirConfig(this);
        }
    }
}
