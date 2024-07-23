package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svole;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Config;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory.Conv32Type;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleFactory;

/**
 * F_3 -> F_2 modulus conversion using Silent VOLE config.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public class SvoleConv32Config extends AbstractMultiPartyPtoConfig implements Conv32Config {
    /**
     * no-choice GF2K-VOLE config
     */
    private final Gf2kNcVoleConfig gf2kNcVoleConfig;

    private SvoleConv32Config(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.gf2kNcVoleConfig);
        gf2kNcVoleConfig = builder.gf2kNcVoleConfig;
    }

    public Gf2kNcVoleConfig getGf2kNcVoleConfig() {
        return gf2kNcVoleConfig;
    }

    @Override
    public Conv32Type getPtoType() {
        return Conv32Type.SVOLE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SvoleConv32Config> {
        /**
         * no-choice GF2K-VOLE config
         */
        private final Gf2kNcVoleConfig gf2kNcVoleConfig;

        public Builder(SecurityModel securityModel) {
            gf2kNcVoleConfig = Gf2kNcVoleFactory.createDefaultConfig(securityModel);
        }

        @Override
        public SvoleConv32Config build() {
            return new SvoleConv32Config(this);
        }
    }
}
