package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp.Z2BspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp.Z2BspVoleFactory;

/**
 * WYKW21-Z2-BSP-VOLE半诚实安全协议。
 *
 * @author Weiran Liu
 * @date 2022/6/22
 */
public class Wykw21ShZ2BspVoleConfig implements Z2BspVoleConfig {
    /**
     * 核COT协议配置项
     */
    private final CoreCotConfig coreCotConfig;

    private Wykw21ShZ2BspVoleConfig(Builder builder) {
        coreCotConfig = builder.coreCotConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    @Override
    public Z2BspVoleFactory.Z2BspVoleType getPtoType() {
        return Z2BspVoleFactory.Z2BspVoleType.WYKW21_SEMI_HONEST;
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

public static class Builder implements org.apache.commons.lang3.builder.Builder<Wykw21ShZ2BspVoleConfig> {
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
    public Wykw21ShZ2BspVoleConfig build() {
        return new Wykw21ShZ2BspVoleConfig(this);
    }
}
}
