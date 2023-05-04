package edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.naive;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtFactory;

/**
 * naive private equality test config.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class NaivePeqtConfig implements PeqtConfig {
    /**
     * Boolean circuit config
     */
    private final BcConfig bcConfig;

    private NaivePeqtConfig(Builder builder) {
        bcConfig = builder.bcConfig;
    }

    public BcConfig getBcConfig() {
        return bcConfig;
    }

    @Override
    public PeqtFactory.PeqtType getPtoType() {
        return PeqtFactory.PeqtType.NAIVE;
    }

    @Override
    public void setEnvType(EnvType envType) {
        bcConfig.setEnvType(envType);
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaivePeqtConfig> {
        /**
         * Boolean circuit config
         */
        private BcConfig bcConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            bcConfig = BcFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setBcConfig(BcConfig bcConfig) {
            this.bcConfig = bcConfig;
            return this;
        }

        @Override
        public NaivePeqtConfig build() {
            return new NaivePeqtConfig(this);
        }
    }
}
