package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.gyw23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleFactory.Gf2kSspVoleType;

/**
 * GYW23 GF2K-SSP-VOLE config.
 *
 * @author Weiran Liu
 * @date 2024/6/8
 */
public class Gyw23Gf2kSspVoleConfig extends AbstractMultiPartyPtoConfig implements Gf2kSspVoleConfig {
    /**
     * core COT
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * core GF2K-VOLE config
     */
    private final Gf2kCoreVoleConfig gf2kCoreVoleConfig;

    private Gyw23Gf2kSspVoleConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig, builder.gf2kCoreVoleConfig);
        coreCotConfig = builder.coreCotConfig;
        gf2kCoreVoleConfig = builder.gf2kCoreVoleConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public Gf2kCoreVoleConfig getGf2kCoreVoleConfig() {
        return gf2kCoreVoleConfig;
    }

    @Override
    public Gf2kSspVoleType getPtoType() {
        return Gf2kSspVoleType.GYW23;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gyw23Gf2kSspVoleConfig> {
        /**
         * core COT
         */
        private final CoreCotConfig coreCotConfig;
        /**
         * core GF2K-VOLE config
         */
        private final Gf2kCoreVoleConfig gf2kCoreVoleConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            gf2kCoreVoleConfig = Gf2kCoreVoleFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public Gyw23Gf2kSspVoleConfig build() {
            return new Gyw23Gf2kSspVoleConfig(this);
        }
    }
}
