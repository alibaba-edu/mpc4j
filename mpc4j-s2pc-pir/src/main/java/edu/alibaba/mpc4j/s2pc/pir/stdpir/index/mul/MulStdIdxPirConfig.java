package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.mul;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;

/**
 * MulPIR config.
 *
 * @author Qixian Zhou
 * @date 2023/5/29
 */
public class MulStdIdxPirConfig extends AbstractMultiPartyPtoConfig implements PbcableStdIdxPirConfig {
    /**
     * MulPIR params
     */
    private final MulStdIdxPirParams params;

    public MulStdIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        this.params = builder.params;
    }

    @Override
    public StdIdxPirFactory.StdIdxPirType getProType() {
        return StdIdxPirFactory.StdIdxPirType.MUL;
    }

    @Override
    public MulStdIdxPirParams getStdIdxPirParams() {
        return params;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<MulStdIdxPirConfig> {
        /**
         * MulPIR params
         */
        private MulStdIdxPirParams params;

        public Builder() {
            params = MulStdIdxPirParams.DEFAULT_PARAMS;
        }

        public Builder setParams(MulStdIdxPirParams params) {
            this.params = params;
            return this;
        }

        @Override
        public MulStdIdxPirConfig build() {
            return new MulStdIdxPirConfig(this);
        }
    }
}

