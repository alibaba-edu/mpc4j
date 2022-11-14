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
     * 乘法三元组比特长度
     */
    private final int l;
    /**
     * 环境类型
     */
    private EnvType envType;

    private IdealZlCoreMtgConfig(Builder builder) {
        l = builder.l;
        envType = EnvType.STANDARD;
    }

    @Override
    public ZlCoreMtgFactory.ZlCoreMtgType getPtoType() {
        return ZlCoreMtgFactory.ZlCoreMtgType.IDEAL;
    }

    @Override
    public int getL() {
        return l;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<IdealZlCoreMtgConfig> {
        /**
         * 乘法三元组比特长度
         */
        private final int l;

        public Builder(int l) {
            assert l > 0 : "l must be greater than 0: " + l;
            this.l = l;
        }

        @Override
        public IdealZlCoreMtgConfig build() {
            return new IdealZlCoreMtgConfig(this);
        }
    }
}
