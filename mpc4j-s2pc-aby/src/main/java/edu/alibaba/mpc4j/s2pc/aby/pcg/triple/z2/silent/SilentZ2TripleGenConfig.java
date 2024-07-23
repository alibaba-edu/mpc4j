package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.silent;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenFactory.Z2TripleGenType;

/**
 * Silent Z2 triple generation config.
 *
 * @author Weiran Liu
 * @date 2024/5/26
 */
public class SilentZ2TripleGenConfig extends AbstractMultiPartyPtoConfig implements Z2TripleGenConfig {
    /**
     * NC-COT
     */
    private final NcCotConfig ncCotConfig;

    private SilentZ2TripleGenConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.ncCotConfig);
        ncCotConfig = builder.ncCotConfig;
    }

    public NcCotConfig getNcCotConfig() {
        return ncCotConfig;
    }

    @Override
    public Z2TripleGenType getPtoType() {
        return Z2TripleGenType.SILENT_COT;
    }

    @Override
    public int defaultRoundNum() {
        return ncCotConfig.maxNum();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SilentZ2TripleGenConfig> {
        /**
         * NC-COT
         */
        private final NcCotConfig ncCotConfig;

        public Builder() {
            ncCotConfig = NcCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public SilentZ2TripleGenConfig build() {
            return new SilentZ2TripleGenConfig(this);
        }
    }
}
