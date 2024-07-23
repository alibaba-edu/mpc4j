package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.onion;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;

/**
 * OnionPIR config.
 *
 * @author Liqiang Peng
 * @date 2022/11/11
 */
public class OnionStdIdxPirConfig extends AbstractMultiPartyPtoConfig implements PbcableStdIdxPirConfig {
    /**
     * OnionPIR params
     */
    private final OnionStdIdxPirParams params;

    public OnionStdIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        this.params = builder.params;
    }

    @Override
    public StdIdxPirFactory.StdIdxPirType getProType() {
        return StdIdxPirFactory.StdIdxPirType.ONION;
    }

    @Override
    public OnionStdIdxPirParams getStdIdxPirParams() {
        return params;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<OnionStdIdxPirConfig> {
        /**
         * OnionPIR params
         */
        private OnionStdIdxPirParams params;

        public Builder() {
            params = OnionStdIdxPirParams.DEFAULT_PARAMS;
        }

        public Builder setParams(OnionStdIdxPirParams params) {
            this.params = params;
            return this;
        }

        @Override
        public OnionStdIdxPirConfig build() {
            return new OnionStdIdxPirConfig(this);
        }
    }
}
