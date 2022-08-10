package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotFactory.SspCotType;

/**
 * YWL20-SSP-COT恶意安全协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/6/7
 */
public class Ywl20MaSspCotConfig implements SspCotConfig {
    /**
     * 核COT协议配置项
     */
    private final CoreCotConfig coreCotConfig;

    private Ywl20MaSspCotConfig(Builder builder) {
        coreCotConfig = builder.coreCotConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    @Override
    public SspCotType getPtoType() {
        return SspCotType.YWL20_MALICIOUS;
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
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20MaSspCotConfig> {
        /**
         * 核COT协议配置项
         */
        private CoreCotConfig coreCotConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.MALICIOUS);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        @Override
        public Ywl20MaSspCotConfig build() {
            return new Ywl20MaSspCotConfig(this);
        }
    }
}