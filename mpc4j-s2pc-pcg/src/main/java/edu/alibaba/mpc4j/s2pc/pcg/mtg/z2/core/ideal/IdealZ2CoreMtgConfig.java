package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.ideal;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;

/**
 * 理想核布尔三元组生成协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/08
 */
public class IdealZ2CoreMtgConfig implements Z2CoreMtgConfig {
    /**
     * 环境类型
     */
    private EnvType envType;

    private IdealZ2CoreMtgConfig(Builder builder) {
        envType = EnvType.STANDARD;
    }

    @Override
    public Z2CoreMtgFactory.Z2CoreMtgType getPtoType() {
        return Z2CoreMtgFactory.Z2CoreMtgType.IDEAL;
    }

    @Override
    public int maxAllowNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void setEnvType(EnvType envType) {
        this.envType = envType;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.IDEAL;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<IdealZ2CoreMtgConfig> {

        public Builder() {

        }

        @Override
        public IdealZ2CoreMtgConfig build() {
            return new IdealZ2CoreMtgConfig(this);
        }
    }
}
