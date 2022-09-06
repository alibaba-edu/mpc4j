package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.BspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.BspCotFactory;

/**
 * YWL20-BSP-COT半诚实安全协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
public class Ywl20ShBspCotConfig implements BspCotConfig {
    /**
     * 核COT协议配置项
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * GF2K-DPPRF协议配置项
     */
    private final DpprfConfig gf2kDpprfConfig;

    private Ywl20ShBspCotConfig(Builder builder) {
        // 两个协议的环境配型必须相同
        assert builder.coreCotConfig.getEnvType().equals(builder.gf2kDpprfConfig.getEnvType());
        coreCotConfig = builder.coreCotConfig;
        gf2kDpprfConfig = builder.gf2kDpprfConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public DpprfConfig getGf2kDpprfConfig() {
        return gf2kDpprfConfig;
    }

    @Override
    public BspCotFactory.BspCotType getPtoType() {
        return BspCotFactory.BspCotType.YWL20_SEMI_HONEST;
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
        if (gf2kDpprfConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = gf2kDpprfConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20ShBspCotConfig> {
        /**
         * 核COT协议配置项
         */
        private CoreCotConfig coreCotConfig;
        /**
         * GF2K-DPPRF协议配置项
         */
        private DpprfConfig gf2kDpprfConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            gf2kDpprfConfig = DpprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setGf2kDpprfConfig(DpprfConfig dpprfConfig) {
            this.gf2kDpprfConfig = dpprfConfig;
            return this;
        }

        @Override
        public Ywl20ShBspCotConfig build() {
            return new Ywl20ShBspCotConfig(this);
        }
    }
}
