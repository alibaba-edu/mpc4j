package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.iknp03;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory.CoreCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;

/**
 * IKNP03-核COT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/13
 */
public class Iknp03CoreCotConfig extends AbstractMultiPartyPtoConfig implements CoreCotConfig {
    /**
     * 基础OT协议
     */
    private final BaseOtConfig baseOtConfig;

    private Iknp03CoreCotConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.baseOtConfig);
        baseOtConfig = builder.baseOtConfig;
    }

    public BaseOtConfig getBaseOtConfig() {
        return baseOtConfig;
    }

    @Override
    public CoreCotType getPtoType() {
        return CoreCotType.IKNP03;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Iknp03CoreCotConfig> {
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
        public Iknp03CoreCotConfig build() {
            return new Iknp03CoreCotConfig(this);
        }
    }
}
