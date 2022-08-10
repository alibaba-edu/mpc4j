package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotFactory.SspCotType;

/**
 * YWL20-SSP-COT半诚实安全协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class Ywl20ShSspCotConfig implements SspCotConfig {
    /**
     * 核COT协议配置项
     */
    private final CoreCotConfig coreCotConfig;

    private Ywl20ShSspCotConfig(Builder builder) {
        coreCotConfig = builder.coreCotConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    @Override
    public SspCotType getPtoType() {
        return SspCotType.YWL20_SEMI_HONEST;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20ShSspCotConfig> {
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
        public Ywl20ShSspCotConfig build() {
            return new Ywl20ShSspCotConfig(this);
        }
    }
}
