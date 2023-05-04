package edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;

/**
 * YWL20-BP-DPPRF config.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class Ywl20BpDpprfConfig implements BpDpprfConfig {
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * pre-compute COT config
     */
    private final PreCotConfig preCotConfig;

    private Ywl20BpDpprfConfig(Builder builder) {
        assert builder.coreCotConfig.getEnvType().equals(builder.preCotConfig.getEnvType());
        coreCotConfig = builder.coreCotConfig;
        preCotConfig = builder.preCotConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public PreCotConfig getPreCotConfig() {
        return preCotConfig;
    }

    @Override
    public BpDpprfFactory.BpDpprfType getPtoType() {
        return BpDpprfFactory.BpDpprfType.YWL20;
    }

    @Override
    public void setEnvType(EnvType envType) {
        coreCotConfig.setEnvType(envType);
        preCotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return coreCotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.MALICIOUS;
        if (coreCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = coreCotConfig.getSecurityModel();
        }
        if (preCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = preCotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20BpDpprfConfig> {
        /**
         * core COT config
         */
        private CoreCotConfig coreCotConfig;
        /**
         * pre-compute COT config
         */
        private PreCotConfig preCotConfig;

        public Builder(SecurityModel securityModel) {
            coreCotConfig = CoreCotFactory.createDefaultConfig(securityModel);
            preCotConfig = PreCotFactory.createDefaultConfig(securityModel);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setPreCotConfig(PreCotConfig preCotConfig) {
            this.preCotConfig = preCotConfig;
            return this;
        }

        @Override
        public Ywl20BpDpprfConfig build() {
            return new Ywl20BpDpprfConfig(this);
        }
    }
}
