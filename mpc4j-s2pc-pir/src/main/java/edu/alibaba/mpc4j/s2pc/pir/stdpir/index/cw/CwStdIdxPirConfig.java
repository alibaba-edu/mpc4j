package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.cw;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;

/**
 * Constant-weight PIR config
 *
 * @author Qixian Zhou
 * @date 2023/6/18
 */
public class CwStdIdxPirConfig extends AbstractMultiPartyPtoConfig implements PbcableStdIdxPirConfig {
    /**
     * Constant-weight PIR params
     */
    private final CwStdIdxPirParams params;

    public CwStdIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        this.params = builder.params;
    }

    @Override
    public StdIdxPirFactory.StdIdxPirType getProType() {
        return StdIdxPirFactory.StdIdxPirType.CW;
    }

    @Override
    public CwStdIdxPirParams getStdIdxPirParams() {
        return params;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<CwStdIdxPirConfig> {
        /**
         * Constant-weight PIR params
         */
        private CwStdIdxPirParams params;

        public Builder() {
            params = CwStdIdxPirParams.DEFAULT_PARAMS;
        }

        public Builder setParams(CwStdIdxPirParams params) {
            this.params = params;
            return this;
        }

        @Override
        public CwStdIdxPirConfig build() {
            return new CwStdIdxPirConfig(this);
        }
    }
}

