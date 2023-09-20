package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.SspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.SspCotFactory;

/**
 * malicious YWL20-SSP-COT config.
 *
 * @author Weiran Liu
 * @date 2023/7/19
 */
public class Ywl20MaSspCotConfig extends AbstractMultiPartyPtoConfig implements SspCotConfig {
    /**
     * core COT
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * SP-DPPRF
     */
    private final SpDpprfConfig spDpprfConfig;

    private Ywl20MaSspCotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.coreCotConfig, builder.spDpprfConfig);
        coreCotConfig = builder.coreCotConfig;
        spDpprfConfig = builder.spDpprfConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public SpDpprfConfig getSpDpprfConfig() {
        return spDpprfConfig;
    }

    @Override
    public SspCotFactory.SspCotType getPtoType() {
        return SspCotFactory.SspCotType.YWL20_MALICIOUS;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20MaSspCotConfig> {
        /**
         * core COT
         */
        private CoreCotConfig coreCotConfig;
        /**
         * SP-DPPRF
         */
        private SpDpprfConfig spDpprfConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.MALICIOUS);
            spDpprfConfig = SpDpprfFactory.createDefaultConfig(SecurityModel.MALICIOUS);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setSpDpprfConfig(SpDpprfConfig spDpprfConfig) {
            this.spDpprfConfig = spDpprfConfig;
            return this;
        }

        @Override
        public Ywl20MaSspCotConfig build() {
            return new Ywl20MaSspCotConfig(this);
        }
    }
}
