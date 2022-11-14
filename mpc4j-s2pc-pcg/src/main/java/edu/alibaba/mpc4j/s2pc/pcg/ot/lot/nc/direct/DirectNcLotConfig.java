package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.CoreLotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.CoreLotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.NcLotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.NcLotFactory;

/**
 * 直接NC-2^l选1-OT协议配置项。
 *
 * @author Hanwen Feng
 * @date 2022/8/18
 */
public class DirectNcLotConfig implements NcLotConfig {
    /**
     * 核2^l选1-OT协议配置项
     */
    private final CoreLotConfig coreLotConfig;

    private DirectNcLotConfig(Builder builder){
        coreLotConfig = builder.coreLotConfig;
    }

    public CoreLotConfig getCoreLotConfig() {
        return coreLotConfig;
    }

    @Override
    public NcLotFactory.NcLotType getPtoType() {
        return NcLotFactory.NcLotType.Direct;
    }

    @Override
    public int maxAllowNum() {
        return 1 << 24;
    }

    @Override
    public void setEnvType(EnvType envType) {
        coreLotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return coreLotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return coreLotConfig.getSecurityModel();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DirectNcLotConfig> {
        /**
         * 核2^l选1-OT协议配置项
         */
        private CoreLotConfig coreLotConfig;

        public Builder(SecurityModel securityModel) {
            coreLotConfig = CoreLotFactory.createDefaultConfig(securityModel);
        }

        public Builder setLotConfig(CoreLotConfig coreLotConfig) {
            this.coreLotConfig = coreLotConfig;
            return this;
        }

        @Override
        public DirectNcLotConfig build() {
            return new DirectNcLotConfig(this);
        }
    }
}
