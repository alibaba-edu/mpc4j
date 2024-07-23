package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;

/**
 * SEAL PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/1/17
 */
public class SealStdIdxPirConfig extends AbstractMultiPartyPtoConfig implements PbcableStdIdxPirConfig {
    /**
     * SEAL PIR params
     */
    private final SealStdIdxPirParams params;

    public SealStdIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        this.params = builder.params;
    }

    @Override
    public StdIdxPirFactory.StdIdxPirType getProType() {
        return StdIdxPirFactory.StdIdxPirType.SEAL;
    }

    @Override
    public SealStdIdxPirParams getStdIdxPirParams() {
        return params;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SealStdIdxPirConfig> {
        /**
         * SEAL PIR params
         */
        private SealStdIdxPirParams params;

        public Builder() {
            params = SealStdIdxPirParams.DEFAULT_PARAMS;
        }

        public Builder setParams(SealStdIdxPirParams params) {
            this.params = params;
            return this;
        }

        @Override
        public SealStdIdxPirConfig build() {
            return new SealStdIdxPirConfig(this);
        }
    }
}
