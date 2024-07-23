package edu.alibaba.mpc4j.s2pc.pso.psu.zcl23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.zcl23.Zcl23PkeMqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;

/**
 * ZCL23-PKE-PSU protocol config.
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class Zcl23PkePsuConfig extends AbstractMultiPartyPtoConfig implements PsuConfig {
    /**
     * ZCL23-PKE-mqRPMT
     */
    private final Zcl23PkeMqRpmtConfig zcl23PkeMqRpmtConfig;
    /**
     * core COT
     */
    private final CoreCotConfig coreCotConfig;

    private Zcl23PkePsuConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.zcl23PkeMqRpmtConfig, builder.coreCotConfig);
        zcl23PkeMqRpmtConfig = builder.zcl23PkeMqRpmtConfig;
        coreCotConfig = builder.coreCotConfig;
    }

    @Override
    public PsuType getPtoType() {
        return PsuType.ZCL23_PKE;
    }

    public Zcl23PkeMqRpmtConfig getZcl23PkeMqRpmtConfig() {
        return zcl23PkeMqRpmtConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Zcl23PkePsuConfig> {
        /**
         * ZCL23-PKE-mqRPMT
         */
        private Zcl23PkeMqRpmtConfig zcl23PkeMqRpmtConfig;
        /**
         * core COT
         */
        private CoreCotConfig coreCotConfig;

        public Builder() {
            zcl23PkeMqRpmtConfig = new Zcl23PkeMqRpmtConfig.Builder().build();
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setZcl23PkeMqRpmtConfig(Zcl23PkeMqRpmtConfig zcl23PkeMqRpmtConfig) {
            this.zcl23PkeMqRpmtConfig = zcl23PkeMqRpmtConfig;
            return this;
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        @Override
        public Zcl23PkePsuConfig build() {
            return new Zcl23PkePsuConfig(this);
        }
    }
}
