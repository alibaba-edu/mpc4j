package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j;

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
public class Seal4jStdIdxPirConfig extends AbstractMultiPartyPtoConfig implements PbcableStdIdxPirConfig {
    /**
     * SEAL PIR params
     */
    private final Seal4jStdIdxPirParams params;

    public Seal4jStdIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        this.params = builder.params;
    }

    @Override
    public StdIdxPirFactory.StdIdxPirType getProType() {
        return StdIdxPirFactory.StdIdxPirType.SEAL4J;
    }

    @Override
    public Seal4jStdIdxPirParams getStdIdxPirParams() {
        return params;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Seal4jStdIdxPirConfig> {
        /**
         * SEAL PIR params
         */
        private Seal4jStdIdxPirParams params;

        public Builder() {
            params = Seal4jStdIdxPirParams.DEFAULT_PARAMS;
        }

        public Builder setParams(Seal4jStdIdxPirParams params) {
            this.params = params;
            return this;
        }

        @Override
        public Seal4jStdIdxPirConfig build() {
            return new Seal4jStdIdxPirConfig(this);
        }
    }
}
