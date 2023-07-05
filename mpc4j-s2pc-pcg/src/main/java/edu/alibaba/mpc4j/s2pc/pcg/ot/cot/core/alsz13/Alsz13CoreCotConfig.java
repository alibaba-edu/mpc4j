package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.alsz13;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory.CoreCotType;

/**
 * ALSZ13-核COT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/13
 */
public class Alsz13CoreCotConfig extends AbstractMultiPartyPtoConfig implements CoreCotConfig {
    /**
     * 基础OT协议
     */
    private final BaseOtConfig baseOtConfig;

    private Alsz13CoreCotConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.baseOtConfig);
        baseOtConfig = builder.baseOtConfig;
    }

    public BaseOtConfig getBaseOtConfig() {
        return baseOtConfig;
    }

    @Override
    public CoreCotType getPtoType() {
        return CoreCotType.ALSZ13;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Alsz13CoreCotConfig> {
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
        public Alsz13CoreCotConfig build() {
            return new Alsz13CoreCotConfig(this);
        }
    }
}
