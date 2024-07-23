package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.gyw23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeFactory.Gf2kBspVodeType;

/**
 * GYW23 GF2K-BSP-VODE config.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Gyw23Gf2kBspVodeConfig extends AbstractMultiPartyPtoConfig implements Gf2kBspVodeConfig {
    /**
     * core COT
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * GF2K-core-VODE config
     */
    private final Gf2kCoreVodeConfig gf2kCoreVodeConfig;

    private Gyw23Gf2kBspVodeConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig, builder.gf2kCoreVodeConfig);
        coreCotConfig = builder.coreCotConfig;
        gf2kCoreVodeConfig = builder.gf2kCoreVodeConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public Gf2kCoreVodeConfig getGf2kCoreVodeConfig() {
        return gf2kCoreVodeConfig;
    }

    @Override
    public Gf2kBspVodeType getPtoType() {
        return Gf2kBspVodeType.GYW23;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gyw23Gf2kBspVodeConfig> {
        /**
         * core COT
         */
        private final CoreCotConfig coreCotConfig;
        /**
         * GF2K-core-VODE config
         */
        private final Gf2kCoreVodeConfig gf2kCoreVodeConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            gf2kCoreVodeConfig = Gf2kCoreVodeFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public Gyw23Gf2kBspVodeConfig build() {
            return new Gyw23Gf2kBspVodeConfig(this);
        }
    }
}
