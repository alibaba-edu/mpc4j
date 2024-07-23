package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.bcg19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeFactory.Gf2kMspVodeType;

/**
 * BCG19-REG-MSP-VODE config.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Bcg19RegGf2kMspVodeConfig extends AbstractMultiPartyPtoConfig implements Gf2kMspVodeConfig {
    /**
     * GF2K-BSP-VODE config
     */
    private final Gf2kBspVodeConfig gf2kBspVodeConfig;

    private Bcg19RegGf2kMspVodeConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.gf2kBspVodeConfig);
        gf2kBspVodeConfig = builder.gf2kBspVodeConfig;
    }

    @Override
    public Gf2kBspVodeConfig getGf2kBspVodeConfig() {
        return gf2kBspVodeConfig;
    }

    @Override
    public Gf2kMspVodeType getPtoType() {
        return Gf2kMspVodeType.BCG19_REG;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bcg19RegGf2kMspVodeConfig> {
        /**
         * GF2K-BSP-VODE config
         */
        private final Gf2kBspVodeConfig gf2kBspVodeConfig;

        public Builder(SecurityModel securityModel) {
            gf2kBspVodeConfig = Gf2kBspVodeFactory.createDefaultConfig(securityModel);
        }

        @Override
        public Bcg19RegGf2kMspVodeConfig build() {
            return new Bcg19RegGf2kMspVodeConfig(this);
        }
    }
}
