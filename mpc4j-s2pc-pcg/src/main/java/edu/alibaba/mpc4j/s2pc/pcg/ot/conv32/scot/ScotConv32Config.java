package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.scot;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Config;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory.Conv32Type;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;

/**
 * F_3 -> F_2 modulus conversion using Silent COT config.
 *
 * @author Weiran Liu
 * @date 2024/6/5
 */
public class ScotConv32Config extends AbstractMultiPartyPtoConfig implements Conv32Config {
    /**
     * no-choice COT config
     */
    private final NcCotConfig ncCotConfig;

    private ScotConv32Config(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.ncCotConfig);
        ncCotConfig = builder.ncCotConfig;
    }

    public NcCotConfig getNcCotConfig() {
        return ncCotConfig;
    }

    @Override
    public Conv32Type getPtoType() {
        return Conv32Type.SCOT;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<ScotConv32Config> {
        /**
         * no-choice COT config
         */
        private final NcCotConfig ncCotConfig;

        public Builder(SecurityModel securityModel) {
            ncCotConfig = NcCotFactory.createDefaultConfig(securityModel);
        }

        @Override
        public ScotConv32Config build() {
            return new ScotConv32Config(this);
        }
    }
}
