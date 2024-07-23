package edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirFactory;

/**
 * Label PSI standard KSPIR config.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class LabelpsiStdKsPirConfig extends AbstractMultiPartyPtoConfig implements StdKsPirConfig {
    /**
     * params
     */
    private final LabelpsiStdKsPirParams params;

    public LabelpsiStdKsPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        params = builder.params;
    }

    @Override
    public StdKsPirFactory.StdKsPirType getPtoType() {
        return StdKsPirFactory.StdKsPirType.Label_PSI;
    }

    public LabelpsiStdKsPirParams getParams() {
        return params;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<LabelpsiStdKsPirConfig> {
        /**
         * params
         */
        private LabelpsiStdKsPirParams params;

        public Builder() {
            params = LabelpsiStdKsPirParams.SERVER_1M_CLIENT_MAX_4096;
        }

        public Builder setParams(LabelpsiStdKsPirParams params) {
            this.params = params;
            return this;
        }

        @Override
        public LabelpsiStdKsPirConfig build() {
            return new LabelpsiStdKsPirConfig(this);
        }
    }
}
