package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.plg24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.ZlDaBitGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.ZlDaBitGenFactory.ZlDaBitGenType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * PLG24 Zl daBit generation config.
 *
 * @author Weiran Liu
 * @date 2024/7/2
 */
public class Plg24ZlDaBitGenConfig extends AbstractMultiPartyPtoConfig implements ZlDaBitGenConfig {
    /**
     * COT config
     */
    private final CotConfig cotConfig;

    private Plg24ZlDaBitGenConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig);
        this.cotConfig = builder.cotConfig;
    }

    @Override
    public ZlDaBitGenType getPtoType() {
        return ZlDaBitGenType.PLG24;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Plg24ZlDaBitGenConfig> {
        /**
         * COT config
         */
        private final CotConfig cotConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            cotConfig = CotFactory.createDefaultConfig(securityModel, silent);
        }

        @Override
        public Plg24ZlDaBitGenConfig build() {
            return new Plg24ZlDaBitGenConfig(this);
        }
    }
}
