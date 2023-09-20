package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.kos15;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64Factory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

/**
 * KOS15-核COT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/30
 */
public class Kos15CoreCotConfig extends AbstractMultiPartyPtoConfig implements CoreCotConfig {
    /**
     * 基础OT协议
     */
    private final BaseOtConfig baseOtConfig;
    /**
     * GF64 type
     */
    private final Gf64Factory.Gf64Type gf64Type;

    private Kos15CoreCotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.baseOtConfig);
        baseOtConfig = builder.baseOtConfig;
        gf64Type = builder.gf64Type;
    }

    public BaseOtConfig getBaseOtConfig() {
        return baseOtConfig;
    }

    @Override
    public CoreCotFactory.CoreCotType getPtoType() {
        return CoreCotFactory.CoreCotType.KOS15;
    }

    public Gf64Factory.Gf64Type getGf64Type() {
        return gf64Type;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kos15CoreCotConfig> {
        /**
         * 基础OT协议配置项
         */
        private BaseOtConfig baseOtConfig;
        /**
         * GF64 type
         */
        private Gf64Factory.Gf64Type gf64Type;

        public Builder() {
            baseOtConfig = BaseOtFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            gf64Type = Gf64Factory.Gf64Type.COMBINED;
        }

        public Builder setBaseOtConfig(BaseOtConfig baseOtConfig) {
            this.baseOtConfig = baseOtConfig;
            return this;
        }

        public Builder setGf64Type(Gf64Factory.Gf64Type gf64Type) {
            this.gf64Type = gf64Type;
            return this;
        }

        @Override
        public Kos15CoreCotConfig build() {
            return new Kos15CoreCotConfig(this);
        }
    }
}
