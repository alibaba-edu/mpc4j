package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.cot;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory.LnotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.NcLnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.NcLnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.PreLnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.PreLnotFactory;

/**
 * cache 1-out-of-n (with n = 2^l) OT config.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class CotLnotConfig extends AbstractMultiPartyPtoConfig implements LnotConfig {
    /**
     * no-choice LNOT config
     */
    private final NcLnotConfig ncLnotConfig;
    /**
     * pre-compute LNOT config
     */
    private final PreLnotConfig preLnotConfig;

    private CotLnotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.ncLnotConfig, builder.preLnotConfig);
        ncLnotConfig = builder.ncLnotConfig;
        preLnotConfig = builder.preLnotConfig;
    }

    public NcLnotConfig getNcLnotConfig() {
        return ncLnotConfig;
    }

    public PreLnotConfig getPreLnotConfig() {
        return preLnotConfig;
    }

    @Override
    public LnotType getPtoType() {
        return LnotType.COT;
    }

    @Override
    public int defaultRoundNum(int l) {
        return ncLnotConfig.maxNum(l);
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<CotLnotConfig> {
        /**
         * no-choice LNOT config
         */
        private final NcLnotConfig ncLnotConfig;
        /**
         * precompute LNOT config
         */
        private final PreLnotConfig preLnotConfig;

        public Builder(SecurityModel securityModel) {
            ncLnotConfig = NcLnotFactory.createDefaultConfig(securityModel);
            preLnotConfig = PreLnotFactory.createDefaultConfig(securityModel);
        }

        @Override
        public CotLnotConfig build() {
            return new CotLnotConfig(this);
        }
    }
}
