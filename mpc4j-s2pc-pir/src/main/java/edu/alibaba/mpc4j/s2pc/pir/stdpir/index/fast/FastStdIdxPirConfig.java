package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.fast;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;

/**
 * FastPIR config.
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class FastStdIdxPirConfig extends AbstractMultiPartyPtoConfig implements PbcableStdIdxPirConfig {
    /**
     * FastPIR params
     */
    private final FastStdIdxPirParams params;

    public FastStdIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        params = builder.params;
    }

    @Override
    public StdIdxPirFactory.StdIdxPirType getProType() {
        return StdIdxPirFactory.StdIdxPirType.FAST;
    }

    @Override
    public FastStdIdxPirParams getStdIdxPirParams() {
        return params;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<FastStdIdxPirConfig> {
        /**
         * FastPIR params
         */
        private FastStdIdxPirParams params;

        public Builder() {
            params = FastStdIdxPirParams.DEFAULT_PARAMS;
        }

        public Builder setParams(FastStdIdxPirParams params) {
            this.params = params;
            return this;
        }

        @Override
        public FastStdIdxPirConfig build() {
            return new FastStdIdxPirConfig(this);
        }
    }
}
