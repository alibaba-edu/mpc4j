package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np99;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BnotFactory;


/**
 * NP99-基础n选1-OT协议配置项。
 *
 * @author Hanwen Feng
 * @date 2022/07/19
 */
public class Np99BnotConfig implements BnotConfig {
    /**
     * 基础OT协议配置项
     */
    private final BaseOtConfig baseOtConfig;

    private Np99BnotConfig(Builder builder) {
        baseOtConfig = builder.baseOtConfig;
    }

    public BaseOtConfig getBaseOtConfig() {
        return baseOtConfig;
    }

    @Override
    public BnotFactory.BnotType getPtoType() {
        return BnotFactory.BnotType.NP99;
    }

    @Override
    public EnvType getEnvType() {
        return baseOtConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return baseOtConfig.getSecurityModel();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Np99BnotConfig> {
        /**
         * 基础OT协议配置项
         */
        private BaseOtConfig baseOtConfig;

        public Builder() {
            baseOtConfig = BaseOtFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setBaseOtConfig(BaseOtConfig baseOtConfig) {
            this.baseOtConfig = baseOtConfig;
            return this;
        }

        @Override
        public Np99BnotConfig build() {
            return new Np99BnotConfig(this);
        }
    }
}
