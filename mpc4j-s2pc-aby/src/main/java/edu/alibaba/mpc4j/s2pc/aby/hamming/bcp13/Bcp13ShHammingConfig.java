package edu.alibaba.mpc4j.s2pc.aby.hamming.bcp13;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingConfig;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * BCP13半诚实安全汉明距离协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/11/22
 */
public class Bcp13ShHammingConfig implements HammingConfig {
    /**
     * COT协议配置项
     */
    private final CotConfig cotConfig;

    private Bcp13ShHammingConfig(Builder builder) {
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public HammingFactory.HammingType getPtoType() {
        return HammingFactory.HammingType.BCP13_SEMI_HONEST;
    }

    @Override
    public int maxAllowBitNum() {
        return cotConfig.maxBaseNum();
    }

    @Override
    public void setEnvType(EnvType envType) {
        cotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return cotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (cotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = cotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bcp13ShHammingConfig> {
        /**
         * COT协议配置项
         */
        private CotConfig cotConfig;

        public Builder() {
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Bcp13ShHammingConfig build() {
            return new Bcp13ShHammingConfig(this);
        }
    }
}
