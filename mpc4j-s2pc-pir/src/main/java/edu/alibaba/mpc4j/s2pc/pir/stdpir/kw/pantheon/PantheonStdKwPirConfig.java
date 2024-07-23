package edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.pantheon;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.StdKwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.StdKwPirFactory;

/**
 * Pantheon standard keyword PIR config.
 *
 * @author Liqiang Peng
 * @date 2024/7/19
 */
public class PantheonStdKwPirConfig extends AbstractMultiPartyPtoConfig implements StdKwPirConfig {
    /**
     * params
     */
    private final PantheonStdKwPirParams params;

    public PantheonStdKwPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        params = builder.params;
    }

    @Override
    public StdKwPirFactory.StdKwPirType getPtoType() {
        return StdKwPirFactory.StdKwPirType.Pantheon;
    }

    public PantheonStdKwPirParams getParams() {
        return params;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<PantheonStdKwPirConfig> {
        /**
         * params
         */
        private PantheonStdKwPirParams params;

        public Builder() {
            params = PantheonStdKwPirParams.DEFAULT_PARAMS;
        }

        public Builder setParams(PantheonStdKwPirParams params) {
            this.params = params;
            return this;
        }

        @Override
        public PantheonStdKwPirConfig build() {
            return new PantheonStdKwPirConfig(this);
        }
    }
}
