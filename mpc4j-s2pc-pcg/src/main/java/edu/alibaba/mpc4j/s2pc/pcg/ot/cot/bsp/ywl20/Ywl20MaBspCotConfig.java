package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

/**
 * YWL20-BSP-COT恶意安全协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/6/7
 */
public class Ywl20MaBspCotConfig implements BspCotConfig {
    /**
     * 核COT协议配置项
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * DPPRF协议配置项
     */
    private final BpDpprfConfig bpDpprfConfig;

    private Ywl20MaBspCotConfig(Builder builder) {
        // 两个协议的环境配型必须相同
        assert builder.coreCotConfig.getEnvType().equals(builder.bpDpprfConfig.getEnvType());
        coreCotConfig = builder.coreCotConfig;
        bpDpprfConfig = builder.bpDpprfConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public BpDpprfConfig getBpDpprfConfig() {
        return bpDpprfConfig;
    }

    @Override
    public BspCotFactory.BspCotType getPtoType() {
        return BspCotFactory.BspCotType.YWL20_MALICIOUS;
    }

    @Override
    public void setEnvType(EnvType envType) {
        coreCotConfig.setEnvType(envType);
        bpDpprfConfig.setEnvType(envType);
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
        if (bpDpprfConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = bpDpprfConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20MaBspCotConfig> {
        /**
         * 核COT协议配置项
         */
        private CoreCotConfig coreCotConfig;
        /**
         * DPPRF协议配置项
         */
        private BpDpprfConfig bpDpprfConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.MALICIOUS);
            bpDpprfConfig = BpDpprfFactory.createDefaultConfig(SecurityModel.MALICIOUS);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setBpDpprfConfig(BpDpprfConfig bpDpprfConfig) {
            this.bpDpprfConfig = bpDpprfConfig;
            return this;
        }

        @Override
        public Ywl20MaBspCotConfig build() {
            return new Ywl20MaBspCotConfig(this);
        }
    }
}
