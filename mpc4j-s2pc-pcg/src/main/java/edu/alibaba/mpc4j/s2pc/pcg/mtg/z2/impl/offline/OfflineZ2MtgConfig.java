package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;

/**
 * 离线布尔三元组生成协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/8
 */
public class OfflineZ2MtgConfig implements Z2MtgConfig {
    /**
     * 核布尔三元组生成协议配置项
     */
    private final Z2CoreMtgConfig z2CoreMtgConfig;

    private OfflineZ2MtgConfig(Builder builder) {
        z2CoreMtgConfig = builder.z2CoreMtgConfig;
    }

    public Z2CoreMtgConfig getZ2CoreMtgConfig() {
        return z2CoreMtgConfig;
    }

    @Override
    public Z2MtgFactory.Z2MtgType getPtoType() {
        return Z2MtgFactory.Z2MtgType.OFFLINE;
    }

    @Override
    public int maxBaseNum() {
        return z2CoreMtgConfig.maxAllowNum();
    }

    @Override
    public void setEnvType(EnvType envType) {
        z2CoreMtgConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return z2CoreMtgConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return z2CoreMtgConfig.getSecurityModel();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<OfflineZ2MtgConfig> {
        /**
         * 核布尔三元组生成协议配置项
         */
        private Z2CoreMtgConfig z2CoreMtgConfig;

        public Builder(SecurityModel securityModel) {
            z2CoreMtgConfig = Z2CoreMtgFactory.createDefaultConfig(securityModel);
        }

        public Builder setZ2CoreMtgConfig(Z2CoreMtgConfig z2CoreMtgConfig) {
            this.z2CoreMtgConfig = z2CoreMtgConfig;
            return this;
        }

        @Override
        public OfflineZ2MtgConfig build() {
            return new OfflineZ2MtgConfig(this);
        }
    }
}