package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.bcg19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleFactory;

/**
 * BCG19-REG-MSP-COT clonfig.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
public class Bcg19RegGf2kMspVoleConfig extends AbstractMultiPartyPtoConfig implements Gf2kMspVoleConfig {
    /**
     * GF2K-BSP-VOLE config
     */
    private final Gf2kBspVoleConfig gf2kBspVoleConfig;

    private Bcg19RegGf2kMspVoleConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.gf2kBspVoleConfig);
        gf2kBspVoleConfig = builder.gf2kBspVoleConfig;
    }

    @Override
    public Gf2kBspVoleConfig getGf2kBspVoleConfig() {
        return gf2kBspVoleConfig;
    }

    @Override
    public Gf2kMspVoleFactory.Gf2kMspVoleType getPtoType() {
        return Gf2kMspVoleFactory.Gf2kMspVoleType.BCG19_REG;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bcg19RegGf2kMspVoleConfig> {
        /**
         * GF2K-BSP-VOLE config
         */
        private Gf2kBspVoleConfig gf2kBspVoleConfig;

        public Builder(SecurityModel securityModel) {
            gf2kBspVoleConfig = Gf2kBspVoleFactory.createDefaultConfig(securityModel);
        }

        public Builder setGf2kBspVoleConfig(Gf2kBspVoleConfig gf2kBspVoleConfig) {
            this.gf2kBspVoleConfig = gf2kBspVoleConfig;
            return this;
        }

        @Override
        public Bcg19RegGf2kMspVoleConfig build() {
            return new Bcg19RegGf2kMspVoleConfig(this);
        }
    }
}
