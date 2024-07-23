package edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.rrkc20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.ZlB2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.ZlB2aFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * RRKC20 Zl boolean to arithmetic protocol config.
 *
 * @author Liqiang Peng
 * @date 2024/6/4
 */
public class Rrkc20ZlB2aConfig extends AbstractMultiPartyPtoConfig implements ZlB2aConfig {
    /**
     * cot config
     */
    private final CotConfig cotConfig;

    private Rrkc20ZlB2aConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig);
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public ZlB2aFactory.ZlB2aType getPtoType() {
        return ZlB2aFactory.ZlB2aType.RRKC20;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrkc20ZlB2aConfig> {
        /**
         * cot config
         */
        private CotConfig cotConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            cotConfig = CotFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Rrkc20ZlB2aConfig build() {
            return new Rrkc20ZlB2aConfig(this);
        }
    }
}
