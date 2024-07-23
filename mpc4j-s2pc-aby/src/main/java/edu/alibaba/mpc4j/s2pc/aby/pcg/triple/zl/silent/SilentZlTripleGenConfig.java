package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.silent;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.ZlTripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.ZlTripleGenFactory.ZlTripleGenType;

/**
 * silent Zl triple generation config.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
public class SilentZlTripleGenConfig extends AbstractMultiPartyPtoConfig implements ZlTripleGenConfig {
    /**
     * NC-COT
     */
    private final NcCotConfig ncCotConfig;

    private SilentZlTripleGenConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.ncCotConfig);
        ncCotConfig = builder.ncCotConfig;
    }

    @Override
    public ZlTripleGenType getPtoType() {
        return ZlTripleGenType.SILENT_COT;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SilentZlTripleGenConfig> {
        /**
         * NC-COT
         */
        private final NcCotConfig ncCotConfig;

        public Builder() {
            ncCotConfig = NcCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public SilentZlTripleGenConfig build() {
            return new SilentZlTripleGenConfig(this);
        }
    }
}
