package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.gyw23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeFactory.Gf2kSspVodeType;

/**
 * GYW23 GF2K-SSP-VODE config.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Gyw23Gf2kSspVodeConfig extends AbstractMultiPartyPtoConfig implements Gf2kSspVodeConfig {
    /**
     * core COT
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * core GF2K-VODE config
     */
    private final Gf2kCoreVodeConfig gf2kCoreVodeConfig;

    private Gyw23Gf2kSspVodeConfig(Builder builder) {
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
    public Gf2kSspVodeType getPtoType() {
        return Gf2kSspVodeType.GYW23;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gyw23Gf2kSspVodeConfig> {
        /**
         * core COT
         */
        private final CoreCotConfig coreCotConfig;
        /**
         * core GF2K-VODE config
         */
        private final Gf2kCoreVodeConfig gf2kCoreVodeConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            gf2kCoreVodeConfig = Gf2kCoreVodeFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public Gyw23Gf2kSspVodeConfig build() {
            return new Gyw23Gf2kSspVodeConfig(this);
        }
    }
}
