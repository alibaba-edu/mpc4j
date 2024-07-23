package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotFactory.SspCotType;

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
    private final SpRdpprfConfig spRdpprfConfig;

    private Ywl20MaSspCotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.coreCotConfig, builder.spRdpprfConfig);
        coreCotConfig = builder.coreCotConfig;
        spRdpprfConfig = builder.spRdpprfConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public SpRdpprfConfig getSpDpprfConfig() {
        return spRdpprfConfig;
    }

    @Override
    public SspCotType getPtoType() {
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
        private SpRdpprfConfig spRdpprfConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.MALICIOUS);
            spRdpprfConfig = SpRdpprfFactory.createDefaultConfig(SecurityModel.MALICIOUS);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setSpDpprfConfig(SpRdpprfConfig spRdpprfConfig) {
            this.spRdpprfConfig = spRdpprfConfig;
            return this;
        }

        @Override
        public Ywl20MaSspCotConfig build() {
            return new Ywl20MaSspCotConfig(this);
        }
    }
}
