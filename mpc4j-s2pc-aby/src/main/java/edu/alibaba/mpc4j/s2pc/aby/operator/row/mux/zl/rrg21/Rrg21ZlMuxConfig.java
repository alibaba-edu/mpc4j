package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.rrg21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * RRG+21 Zl mux config.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public class Rrg21ZlMuxConfig extends AbstractMultiPartyPtoConfig implements ZlMuxConfig {
    /**
     * COT config
     */
    private final CotConfig cotConfig;

    private Rrg21ZlMuxConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig);
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public ZlMuxFactory.ZlMuxType getPtoType() {
        return ZlMuxFactory.ZlMuxType.RRG21;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrg21ZlMuxConfig> {
        /**
         * COT config
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
        public Rrg21ZlMuxConfig build() {
            return new Rrg21ZlMuxConfig(this);
        }
    }
}
