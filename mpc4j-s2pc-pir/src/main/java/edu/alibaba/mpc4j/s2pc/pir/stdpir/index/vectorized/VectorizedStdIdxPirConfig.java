package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;

/**
 * Vectorized Batch PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class VectorizedStdIdxPirConfig extends AbstractMultiPartyPtoConfig implements StdIdxPirConfig {
    /**
     * Vectorized Batch PIR params
     */
    private final VectorizedStdIdxPirParams params;

    public VectorizedStdIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        this.params = builder.params;
    }

    @Override
    public StdIdxPirFactory.StdIdxPirType getProType() {
        return StdIdxPirFactory.StdIdxPirType.VECTOR;
    }

    public VectorizedStdIdxPirParams getStdIdxPirParams() {
        return params;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<VectorizedStdIdxPirConfig> {
        /**
         * Vectorized Batch PIR params
         */
        private VectorizedStdIdxPirParams params;

        public Builder() {
            params = VectorizedStdIdxPirParams.DEFAULT_PARAMS;
        }

        public Builder setParams(VectorizedStdIdxPirParams params) {
            this.params = params;
            return this;
        }

        @Override
        public VectorizedStdIdxPirConfig build() {
            return new VectorizedStdIdxPirConfig(this);
        }
    }
}
