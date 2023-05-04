package edu.alibaba.mpc4j.s2pc.opf.psm.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;

/**
 * CGS22 1-out-of-n (with n = 2^l) OT based PSM config.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class Cgs22LnotPsmConfig implements PsmConfig {
    /**
     * Boolean circuit config
     */
    private final BcConfig bcConfig;
    /**
     * LNOT config
     */
    private final LnotConfig lnotConfig;

    private Cgs22LnotPsmConfig(Builder builder) {
        assert builder.bcConfig.getEnvType().equals(builder.lnotConfig.getEnvType());
        bcConfig = builder.bcConfig;
        lnotConfig = builder.lnotConfig;
    }

    public BcConfig getBcConfig() {
        return bcConfig;
    }

    public LnotConfig getLnotConfig() {
        return lnotConfig;
    }

    @Override
    public PsmFactory.PsmType getPtoType() {
        return PsmFactory.PsmType.CGS22_LNOT;
    }

    @Override
    public void setEnvType(EnvType envType) {
        bcConfig.setEnvType(envType);
        lnotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return bcConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.MALICIOUS;
        if (bcConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = bcConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgs22LnotPsmConfig> {
        /**
         * Boolean circuit config
         */
        private BcConfig bcConfig;
        /**
         * LNOT config
         */
        private LnotConfig lnotConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            bcConfig = BcFactory.createDefaultConfig(securityModel, silent);
            if (silent) {
                lnotConfig = LnotFactory.createCacheConfig(securityModel);
            } else {
                lnotConfig = LnotFactory.createDirectConfig(securityModel);
            }
        }

        public Builder setBcConfig(BcConfig bcConfig) {
            this.bcConfig = bcConfig;
            return this;
        }

        public Builder setLnotConfig(LnotConfig lnotConfig) {
            this.lnotConfig = lnotConfig;
            return this;
        }

        @Override
        public Cgs22LnotPsmConfig build() {
            return new Cgs22LnotPsmConfig(this);
        }
    }
}
