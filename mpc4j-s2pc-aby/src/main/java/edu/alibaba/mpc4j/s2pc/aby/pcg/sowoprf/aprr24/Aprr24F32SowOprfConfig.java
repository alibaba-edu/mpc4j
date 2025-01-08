package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32SowOprfConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32SowOprfFactory.F32SowOprfType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32WprfMatrixFactory.F32WprfMatrixType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Config;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory.Conv32Type;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

/**
 * APRR24 (F3, F2)-sowOPRF config.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public class Aprr24F32SowOprfConfig extends AbstractMultiPartyPtoConfig implements F32SowOprfConfig {
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * F_3 -> F_2 modulus conversion config
     */
    private final Conv32Config conv32Config;
    /**
     * matrix type
     */
    private final F32WprfMatrixType f32WprfMatrixType;

    private Aprr24F32SowOprfConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig, builder.conv32Config);
        coreCotConfig = builder.coreCotConfig;
        conv32Config = builder.conv32Config;
        f32WprfMatrixType = builder.f32WprfMatrixType;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public Conv32Config getConv32Config() {
        return conv32Config;
    }

    @Override
    public F32SowOprfType getPtoType() {
        return F32SowOprfType.APRR24;
    }

    @Override
    public F32WprfMatrixType getMatrixType() {
        return f32WprfMatrixType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Aprr24F32SowOprfConfig> {
        /**
         * core COT config
         */
        private final CoreCotConfig coreCotConfig;
        /**
         * F_3 -> F_2 modulus conversion config
         */
        private final Conv32Config conv32Config;
        /**
         * matrix type
         */
        private F32WprfMatrixType f32WprfMatrixType;

        public Builder(Conv32Type conv32Type) {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            conv32Config = Conv32Factory.createDefaultConfig(SecurityModel.SEMI_HONEST, conv32Type);
            f32WprfMatrixType = F32WprfMatrixType.LONG;
        }

        public Builder setMatrixType(F32WprfMatrixType f32WprfMatrixType) {
            this.f32WprfMatrixType = f32WprfMatrixType;
            return this;
        }

        @Override
        public Aprr24F32SowOprfConfig build() {
            return new Aprr24F32SowOprfConfig(this);
        }
    }
}
