package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.alsz13;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;

/**
 * ALSZ13-核布尔三元组生成协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/6
 */
public class Alsz13Z2CoreMtgConfig implements Z2CoreMtgConfig {
    /**
     * NC-COT协议配置项
     */
    private final NcCotConfig ncCotConfig;

    private Alsz13Z2CoreMtgConfig(Builder builder) {
        ncCotConfig = builder.ncCotConfig;
    }

    public NcCotConfig getNcCotConfig() {
        return ncCotConfig;
    }

    @Override
    public Z2CoreMtgFactory.Z2CoreMtgType getPtoType() {
        return Z2CoreMtgFactory.Z2CoreMtgType.ALSZ13;
    }

    @Override
    public int maxAllowNum() {
        return ncCotConfig.maxAllowNum();
    }

    @Override
    public void setEnvType(EnvType envType) {
        ncCotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return ncCotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (ncCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = ncCotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Alsz13Z2CoreMtgConfig> {
        /**
         * NC-COT协议配置项
         */
        private NcCotConfig ncCotConfig;

        public Builder() {
            ncCotConfig = NcCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setNcCotConfig(NcCotConfig ncCotConfig) {
            this.ncCotConfig = ncCotConfig;
            return this;
        }

        @Override
        public Alsz13Z2CoreMtgConfig build() {
            return new Alsz13Z2CoreMtgConfig(this);
        }
    }
}
