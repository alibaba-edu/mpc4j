package edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.bcp13;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.HammingConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.HammingFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * BCP13半诚实安全汉明距离协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/11/22
 */
public class Bcp13ShHammingConfig extends AbstractMultiPartyPtoConfig implements HammingConfig {
    /**
     * COT协议配置项
     */
    private final CotConfig cotConfig;

    private Bcp13ShHammingConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig);
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public HammingFactory.HammingType getPtoType() {
        return HammingFactory.HammingType.BCP13_SEMI_HONEST;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bcp13ShHammingConfig> {
        /**
         * COT协议配置项
         */
        private CotConfig cotConfig;

        public Builder() {
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Bcp13ShHammingConfig build() {
            return new Bcp13ShHammingConfig(this);
        }
    }
}
