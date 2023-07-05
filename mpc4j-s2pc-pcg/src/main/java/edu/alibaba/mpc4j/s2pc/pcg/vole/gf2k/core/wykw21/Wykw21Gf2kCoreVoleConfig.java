package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;

/**
 * WYKW21-GF2K-core VOLE config.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Wykw21Gf2kCoreVoleConfig extends AbstractMultiPartyPtoConfig implements Gf2kCoreVoleConfig {
    /**
     * the base OT config.
     */
    private final BaseOtConfig baseOtConfig;

    private Wykw21Gf2kCoreVoleConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.baseOtConfig);
        baseOtConfig = builder.baseOtConfig;
    }

    public BaseOtConfig getBaseOtConfig() {
        return baseOtConfig;
    }

    @Override
    public Gf2kCoreVoleFactory.Gf2kCoreVoleType getPtoType() {
        return Gf2kCoreVoleFactory.Gf2kCoreVoleType.WYKW21;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Wykw21Gf2kCoreVoleConfig> {
        /**
         * the base OT config
         */
        private BaseOtConfig baseOtConfig;

        public Builder() {
            baseOtConfig = BaseOtFactory.createDefaultConfig(SecurityModel.MALICIOUS);
        }

        public Builder setBaseOtConfig(BaseOtConfig baseOtConfig) {
            this.baseOtConfig = baseOtConfig;
            return this;
        }

        @Override
        public Wykw21Gf2kCoreVoleConfig build() {
            return new Wykw21Gf2kCoreVoleConfig(this);
        }
    }
}
