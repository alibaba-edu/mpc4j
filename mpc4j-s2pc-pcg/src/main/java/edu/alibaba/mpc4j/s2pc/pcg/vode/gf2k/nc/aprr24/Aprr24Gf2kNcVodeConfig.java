package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.aprr24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.Gf2kNcVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.Gf2kNcVodeFactory.Gf2kNcVodeType;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeFactory;

/**
 * APRR24 GF2K-NC-VODE config.
 *
 * @author Weiran Liu
 * @date 2024/6/13
 */
public class Aprr24Gf2kNcVodeConfig extends AbstractMultiPartyPtoConfig implements Gf2kNcVodeConfig {
    /**
     * core GF2K-VODE config
     */
    private final Gf2kCoreVodeConfig coreVodeConfig;
    /**
     * GF2K-MSP-VODE config
     */
    private final Gf2kMspVodeConfig mspVodeConfig;

    private Aprr24Gf2kNcVodeConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.coreVodeConfig, builder.mspVodeConfig);
        coreVodeConfig = builder.coreVodeConfig;
        mspVodeConfig = builder.mspVodeConfig;
    }

    public Gf2kCoreVodeConfig getCoreVodeConfig() {
        return coreVodeConfig;
    }

    public Gf2kMspVodeConfig getMspVodeConfig() {
        return mspVodeConfig;
    }

    @Override
    public Gf2kNcVodeType getPtoType() {
        return Gf2kNcVodeType.APRR24;
    }

    @Override
    public int maxNum() {
        return 1 << Aprr24Gf2kNcVodePtoDesc.MAX_LOG_N;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Aprr24Gf2kNcVodeConfig> {
        /**
         * core GF2K-VODE config
         */
        private final Gf2kCoreVodeConfig coreVodeConfig;
        /**
         * GF2K-MSP-VODE config
         */
        private final Gf2kMspVodeConfig mspVodeConfig;

        public Builder(SecurityModel securityModel) {
            coreVodeConfig = Gf2kCoreVodeFactory.createDefaultConfig(securityModel);
            mspVodeConfig = Gf2kMspVodeFactory.createDefaultConfig(securityModel);
        }

        @Override
        public Aprr24Gf2kNcVodeConfig build() {
            return new Aprr24Gf2kNcVodeConfig(this);
        }
    }
}
