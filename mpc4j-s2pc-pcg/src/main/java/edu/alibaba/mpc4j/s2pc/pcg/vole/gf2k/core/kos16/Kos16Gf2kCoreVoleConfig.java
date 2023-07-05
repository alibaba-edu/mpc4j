package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.kos16;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;

/**
 * KOS16-GF2K-core VOLE config.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Kos16Gf2kCoreVoleConfig extends AbstractMultiPartyPtoConfig implements Gf2kCoreVoleConfig {
    /**
     * the base OT config.
     */
    private final BaseOtConfig baseOtConfig;

    private Kos16Gf2kCoreVoleConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.baseOtConfig);
        baseOtConfig = builder.baseOtConfig;
    }

    public BaseOtConfig getBaseOtConfig() {
        return baseOtConfig;
    }

    @Override
    public Gf2kCoreVoleFactory.Gf2kCoreVoleType getPtoType() {
        return Gf2kCoreVoleFactory.Gf2kCoreVoleType.KOS16;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kos16Gf2kCoreVoleConfig> {
        /**
         * the base OT config
         */
        private BaseOtConfig baseOtConfig;

        public Builder() {
            baseOtConfig = BaseOtFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setBaseOtConfig(BaseOtConfig baseOtConfig) {
            this.baseOtConfig = baseOtConfig;
            return this;
        }

        @Override
        public Kos16Gf2kCoreVoleConfig build() {
            return new Kos16Gf2kCoreVoleConfig(this);
        }
    }
}
