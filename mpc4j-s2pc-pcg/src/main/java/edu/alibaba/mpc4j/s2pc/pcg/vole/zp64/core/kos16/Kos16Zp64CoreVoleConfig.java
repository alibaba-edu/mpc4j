package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core.kos16;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core.Zp64CoreVoleFactory.Zp64CoreVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core.Zp64CoreVoleConfig;

/**
 * KOS16-Zp64-核VOLE协议配置项。
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public class Kos16Zp64CoreVoleConfig extends AbstractMultiPartyPtoConfig implements Zp64CoreVoleConfig {
    /**
     * 基础OT协议
     */
    private final BaseOtConfig baseOtConfig;

    private Kos16Zp64CoreVoleConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.baseOtConfig);
        baseOtConfig = builder.baseOtConfig;
    }

    public BaseOtConfig getBaseOtConfig() {
        return baseOtConfig;
    }

    @Override
    public Zp64CoreVoleType getPtoType() {
        return Zp64CoreVoleType.KOS16;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kos16Zp64CoreVoleConfig> {
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
        public Kos16Zp64CoreVoleConfig build() {
            return new Kos16Zp64CoreVoleConfig(this);
        }
    }

}
