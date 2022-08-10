package edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.kk13;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.LotFactory;

/**
 * KK13-LHOT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
public class Kk13OriLhotConfig implements LhotConfig {
    /**
     * 核COT协议配置项
     */
    private final CoreCotConfig coreCotConfig;

    private Kk13OriLhotConfig(Builder builder) {
        coreCotConfig = builder.coreCotConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    @Override
    public LotFactory.LotType getPtoType() {
        return LotFactory.LotType.KK13_ORI;
    }

    @Override
    public EnvType getEnvType() {
        return coreCotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (coreCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = coreCotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kk13OriLhotConfig> {
        /**
         * 核COT协议配置项
         */
        private CoreCotConfig coreCotConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        @Override
        public Kk13OriLhotConfig build() {
            return new Kk13OriLhotConfig(this);
        }
    }
}
