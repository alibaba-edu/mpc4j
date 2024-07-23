package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svode;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Config;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory.Conv32Type;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.Gf2kNcVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.Gf2kNcVodeFactory;

/**
 * F_3 -> F_2 modulus conversion using Silent VODE config.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public class SvodeConv32Config extends AbstractMultiPartyPtoConfig implements Conv32Config {
    /**
     * GF2K-NC-VODE config
     */
    private final Gf2kNcVodeConfig gf2kNcVodeConfig;

    private SvodeConv32Config(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.gf2kNcVodeConfig);
        gf2kNcVodeConfig = builder.gf2kNcVodeConfig;
    }

    public Gf2kNcVodeConfig getGf2kNcVodeConfig() {
        return gf2kNcVodeConfig;
    }

    @Override
    public Conv32Type getPtoType() {
        return Conv32Type.SVODE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SvodeConv32Config> {
        /**
         * GF2K-NC-VODE config
         */
        private final Gf2kNcVodeConfig gf2kNcVodeConfig;

        public Builder(SecurityModel securityModel) {
            gf2kNcVodeConfig = Gf2kNcVodeFactory.createDefaultConfig(securityModel);
        }

        @Override
        public SvodeConv32Config build() {
            return new SvodeConv32Config(this);
        }
    }
}
