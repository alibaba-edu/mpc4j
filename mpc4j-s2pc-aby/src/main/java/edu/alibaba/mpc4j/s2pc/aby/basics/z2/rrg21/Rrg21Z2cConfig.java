package edu.alibaba.mpc4j.s2pc.aby.basics.z2.rrg21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * RRG+21 Z2 circuit config.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class Rrg21Z2cConfig extends AbstractMultiPartyPtoConfig implements Z2cConfig {
    /**
     * COT config
     */
    private final CotConfig cotConfig;

    private Rrg21Z2cConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig);
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public Z2cFactory.BcType getPtoType() {
        return Z2cFactory.BcType.RRG21;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrg21Z2cConfig> {
        /**
         * no-choice COT config
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
        public Rrg21Z2cConfig build() {
            return new Rrg21Z2cConfig(this);
        }
    }
}
