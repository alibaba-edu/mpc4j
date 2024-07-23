package edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.rrkc20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.ZlWrapConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.ZlWrapFactory;

/**
 * RRKC20 Zl wrap protocol config.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public class Rrkc20ZlWrapConfig extends AbstractMultiPartyPtoConfig implements ZlWrapConfig {
    /**
     * millionaire config
     */
    private final MillionaireConfig millionaireConfig;

    private Rrkc20ZlWrapConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.millionaireConfig);
        millionaireConfig = builder.millionaireConfig;
    }

    public MillionaireConfig getMillionaireConfig() {
        return millionaireConfig;
    }

    @Override
    public ZlWrapFactory.ZlWrapType getPtoType() {
        return ZlWrapFactory.ZlWrapType.RRKC20;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrkc20ZlWrapConfig> {
        /**
         * millionaire config
         */
        private MillionaireConfig millionaireConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            millionaireConfig = MillionaireFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setZlMillionaireConfig(MillionaireConfig millionaireConfig) {
            this.millionaireConfig = millionaireConfig;
            return this;
        }

        @Override
        public Rrkc20ZlWrapConfig build() {
            return new Rrkc20ZlWrapConfig(this);
        }
    }
}
