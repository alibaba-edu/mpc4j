package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.aprr24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeFactory.Gf2kCoreVodeType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.kos16.Kos16Gf2kCoreVoleConfig;

/**
 * APRR24 GF2K-core-VODE config.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
public class Aprr24Gf2kCoreVodeConfig extends AbstractMultiPartyPtoConfig implements Gf2kCoreVodeConfig {
    /**
     * base OT
     */
    private final BaseOtConfig baseOtConfig;

    private Aprr24Gf2kCoreVodeConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.baseOtConfig);
        baseOtConfig = builder.baseOtConfig;
    }

    public BaseOtConfig getBaseOtConfig() {
        return baseOtConfig;
    }

    @Override
    public Gf2kCoreVodeType getPtoType() {
        return Gf2kCoreVodeType.APRR24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Aprr24Gf2kCoreVodeConfig> {
        /**
         * base OT
         */
        private final BaseOtConfig baseOtConfig;

        public Builder() {
            baseOtConfig = BaseOtFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public Aprr24Gf2kCoreVodeConfig build() {
            return new Aprr24Gf2kCoreVodeConfig(this);
        }
    }
}
