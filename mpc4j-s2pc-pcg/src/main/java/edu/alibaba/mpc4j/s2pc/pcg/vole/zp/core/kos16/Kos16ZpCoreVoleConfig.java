package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.kos16;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.ZpCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.ZpCoreVoleFactory;

/**
 * KOS16-ZP-核VOLE协议配置项。
 *
 * @author Hanwen Feng
 * @date 2022/06/08
 */
public class Kos16ZpCoreVoleConfig extends AbstractMultiPartyPtoConfig implements ZpCoreVoleConfig {
    /**
     * 基础OT协议
     */
    private final BaseOtConfig baseOtConfig;

    private Kos16ZpCoreVoleConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.baseOtConfig);
        baseOtConfig = builder.baseOtConfig;
    }

    public BaseOtConfig getBaseOtConfig() {
        return baseOtConfig;
    }

    @Override
    public ZpCoreVoleFactory.ZpCoreVoleType getPtoType() {
        return ZpCoreVoleFactory.ZpCoreVoleType.KOS16;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kos16ZpCoreVoleConfig> {
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
        public Kos16ZpCoreVoleConfig build() {
            return new Kos16ZpCoreVoleConfig(this);
        }
    }

}
