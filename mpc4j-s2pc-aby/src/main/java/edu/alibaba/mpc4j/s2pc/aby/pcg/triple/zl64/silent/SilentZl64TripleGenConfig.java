package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.silent;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.Zl64TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.Zl64TripleGenFactory.Zl64TripleGenType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;

/**
 * Silent Zl64 triple generation config.
 *
 * @author Weiran Liu
 * @date 2024/7/1
 */
public class SilentZl64TripleGenConfig extends AbstractMultiPartyPtoConfig implements Zl64TripleGenConfig {
    /**
     * NC-COT
     */
    private final NcCotConfig ncCotConfig;

    private SilentZl64TripleGenConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.ncCotConfig);
        ncCotConfig = builder.ncCotConfig;
    }

    @Override
    public Zl64TripleGenType getPtoType() {
        return Zl64TripleGenType.SILENT_COT;
    }

    @Override
    public int defaultRoundNum(int l) {
        return (int) Math.floor((double) ncCotConfig.maxNum() / l);
    }

    /**
     * Gets max NC-COT num.
     *
     * @param roundNum round num.
     * @param l        l.
     * @return NC-COT num.
     */
    static int maxNcCotNum(int roundNum, int l) {
        return roundNum * l;
    }

    public NcCotConfig getNcCotConfig() {
        return ncCotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SilentZl64TripleGenConfig> {
        /**
         * NC-COT
         */
        private final NcCotConfig ncCotConfig;

        public Builder() {
            ncCotConfig = NcCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public SilentZl64TripleGenConfig build() {
            return new SilentZl64TripleGenConfig(this);
        }
    }
}
