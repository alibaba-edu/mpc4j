package edu.alibaba.mpc4j.s2pc.pso.psu.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.OoPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;

/**
 * GMR21-PSU config.
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
public class Gmr21PsuConfig extends AbstractMultiPartyPtoConfig implements OoPsuConfig {
    /**
     * GMR21-mqRPMT config
     */
    private final Gmr21MqRpmtConfig gmr21MqRpmtConfig;
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;

    private Gmr21PsuConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.gmr21MqRpmtConfig, builder.coreCotConfig);
        gmr21MqRpmtConfig = builder.gmr21MqRpmtConfig;
        coreCotConfig = builder.coreCotConfig;
    }

    @Override
    public PsuType getPtoType() {
        return PsuType.GMR21;
    }

    public Gmr21MqRpmtConfig getGmr21MqRpmtConfig() {
        return gmr21MqRpmtConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gmr21PsuConfig> {
        /**
         * GMR21-mqRPMT config
         */
        private Gmr21MqRpmtConfig gmr21MqRpmtConfig;
        /**
         * core COT config
         */
        private final CoreCotConfig coreCotConfig;

        public Builder(boolean silent) {
            gmr21MqRpmtConfig = new Gmr21MqRpmtConfig.Builder(silent).build();
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setGmr21MqRpmtConfig(Gmr21MqRpmtConfig gmr21MqRpmtConfig) {
            this.gmr21MqRpmtConfig = gmr21MqRpmtConfig;
            return this;
        }

        @Override
        public Gmr21PsuConfig build() {
            return new Gmr21PsuConfig(this);
        }
    }
}
