package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ideal;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgFactory;

/**
 * 理想核l比特三元组生成协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/08
 */
public class IdealZlCoreMtgConfig implements ZlCoreMtgConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;

    private IdealZlCoreMtgConfig(Builder builder) {
        envType = builder.envType;
    }

    @Override
    public ZlCoreMtgFactory.ZlCoreMtgType getPtoType() {
        return ZlCoreMtgFactory.ZlCoreMtgType.IDEAL;
    }

    @Override
    public int maxAllowNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.IDEAL;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<IdealZlCoreMtgConfig> {
        /**
         * 环境类型
         */
        private EnvType envType;

        public Builder() {
            super();
            this.envType = EnvType.STANDARD;
        }

        public Builder setEnvType(EnvType envType) {
            this.envType = envType;
            return this;
        }

        @Override
        public IdealZlCoreMtgConfig build() {
            return new IdealZlCoreMtgConfig(this);
        }
    }
}
