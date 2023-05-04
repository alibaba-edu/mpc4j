package edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * RRK+20 Zl mux config.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public class Rrk20ZlMuxConfig implements ZlMuxConfig {
    /**
     * COT config
     */
    private final CotConfig cotConfig;

    private Rrk20ZlMuxConfig(Builder builder) {
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public ZlMuxFactory.ZlMuxType getPtoType() {
        return ZlMuxFactory.ZlMuxType.RRK20;
    }

    @Override
    public void setEnvType(EnvType envType) {
        cotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return cotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (cotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = cotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrk20ZlMuxConfig> {
        /**
         * COT config
         */
        private CotConfig cotConfig;

        public Builder() {
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Rrk20ZlMuxConfig build() {
            return new Rrk20ZlMuxConfig(this);
        }
    }
}
