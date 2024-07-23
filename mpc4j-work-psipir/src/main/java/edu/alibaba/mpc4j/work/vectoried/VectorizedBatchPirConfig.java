package edu.alibaba.mpc4j.work.vectoried;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirConfig;
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
    private final VectorizedStdIdxPirConfig vectorizedBatchPirConfig;

    public VectorizedBatchPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.vectorizedBatchPirConfig);
        vectorizedBatchPirConfig = builder.vectorizedBatchPirConfig;
    }

    @Override
    public BatchPirFactory.BatchIndexPirType getPtoType() {
        return BatchPirFactory.BatchIndexPirType.VECTORIZED_BATCH_PIR;
    }

    public VectorizedStdIdxPirConfig getVectorizedBatchPirConfig() {
        return vectorizedBatchPirConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<VectorizedBatchPirConfig> {
        /**
         * vectorized batch PIR config
         */
        private VectorizedStdIdxPirConfig vectorizedBatchPirConfig;

        public Builder() {
            vectorizedBatchPirConfig = new VectorizedStdIdxPirConfig.Builder().build();
        }

        public Builder setVectorizedBatchPirConfig(VectorizedStdIdxPirConfig config) {
            this.vectorizedBatchPirConfig = config;
            return this;
        }

        @Override
        public VectorizedBatchPirConfig build() {
            return new VectorizedBatchPirConfig(this);
        }
    }
}
