package edu.alibaba.mpc4j.s2pc.pir.index.batch.naive;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SimpleSingleIndexPirConfig;

/**
 * Naive Batch PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/7/14
 */
public class NaiveBatchIndexPirConfig extends AbstractMultiPartyPtoConfig implements BatchIndexPirConfig {
    /**
     * single index PIR config
     */
    private final SingleIndexPirConfig singleIndexPirConfig;

    public NaiveBatchIndexPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.singleIndexPirConfig);
        singleIndexPirConfig = builder.singleIndexPirConfig;
    }

    public SingleIndexPirConfig getSingleIndexPirConfig() {
        return singleIndexPirConfig;
    }

    @Override
    public BatchIndexPirFactory.BatchIndexPirType getPtoType() {
        return BatchIndexPirFactory.BatchIndexPirType.NAIVE_BATCH_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaiveBatchIndexPirConfig> {
        /**
         * single index PIR config
         */
        private SingleIndexPirConfig singleIndexPirConfig;

        public Builder() {
            singleIndexPirConfig = new Hhcm23SimpleSingleIndexPirConfig.Builder().build();
        }

        public Builder setSingleIndexPirConfig(SingleIndexPirConfig singleIndexPirConfig) {
            this.singleIndexPirConfig = singleIndexPirConfig;
            return this;
        }

        @Override
        public NaiveBatchIndexPirConfig build() {
            return new NaiveBatchIndexPirConfig(this);
        }
    }
}

