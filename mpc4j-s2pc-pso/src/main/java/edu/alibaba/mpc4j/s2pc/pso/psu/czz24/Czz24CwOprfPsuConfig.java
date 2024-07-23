package edu.alibaba.mpc4j.s2pc.pso.psu.czz24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;

import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz24.Czz24CwOprfMqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;

/**
 * CZZ24-cwOPRF-PSU config.
 *
 * @author Yufei Wang
 * @date 2023/8/1
 */
public class Czz24CwOprfPsuConfig extends AbstractMultiPartyPtoConfig implements PsuConfig {
    /**
     * CZZ24-cwOPRF-mqRPMT
     */
    private final Czz24CwOprfMqRpmtConfig czz24CwOprfPsuConfig;
    /**
     * core COT
     */
    private final CoreCotConfig coreCotConfig;

    private Czz24CwOprfPsuConfig(Czz24CwOprfPsuConfig.Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.czz24CwOprfMqRpmtConfig, builder.coreCotConfig);
        czz24CwOprfPsuConfig = builder.czz24CwOprfMqRpmtConfig;
        coreCotConfig = builder.coreCotConfig;
    }

    @Override
    public PsuType getPtoType() {
        return PsuType.CZZ24_CW_OPRF;
    }

    public Czz24CwOprfMqRpmtConfig getCzz24CwOprfPsuConfig() {
        return czz24CwOprfPsuConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Czz24CwOprfPsuConfig> {
        /**
         * CZZ24-cwOPRF-mqRPMT
         */
        private Czz24CwOprfMqRpmtConfig czz24CwOprfMqRpmtConfig;
        /**
         * core COT
         */
        private CoreCotConfig coreCotConfig;

        public Builder() {
            czz24CwOprfMqRpmtConfig = new Czz24CwOprfMqRpmtConfig.Builder().build();
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setCzz24CwOprfMqRpmtConfig(Czz24CwOprfMqRpmtConfig czz24CwOprfMqRpmtConfig) {
            this.czz24CwOprfMqRpmtConfig = czz24CwOprfMqRpmtConfig;
            return this;
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        @Override
        public Czz24CwOprfPsuConfig build() {
            return new Czz24CwOprfPsuConfig(this);
        }
    }
}
