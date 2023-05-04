package edu.alibaba.mpc4j.s2pc.opf.psm.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmFactory;

/**
 * CGS22 OPPRF-based PSM config.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class Cgs22OpprfPsmConfig implements PsmConfig {
    /**
     * batched OPPRF config
     */
    private final BopprfConfig bopprfConfig;
    /**
     * PEQT config
     */
    private final PeqtConfig peqtConfig;

    private Cgs22OpprfPsmConfig(Builder builder) {
        assert builder.bopprfConfig.getEnvType().equals(builder.peqtConfig.getEnvType());
        bopprfConfig = builder.bopprfConfig;
        peqtConfig = builder.peqtConfig;
    }

    public BopprfConfig getBopprfConfig() {
        return bopprfConfig;
    }

    public PeqtConfig getPeqtConfig() {
        return peqtConfig;
    }

    @Override
    public PsmFactory.PsmType getPtoType() {
        return PsmFactory.PsmType.CGS22_OPPRF;
    }

    @Override
    public void setEnvType(EnvType envType) {
        bopprfConfig.setEnvType(envType);
        peqtConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return bopprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.MALICIOUS;
        if (bopprfConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = bopprfConfig.getSecurityModel();
        }
        if (peqtConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = peqtConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgs22OpprfPsmConfig> {
        /**
         * batched OPPRF config
         */
        private BopprfConfig bopprfConfig;
        /**
         * PEQT config
         */
        private PeqtConfig peqtConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            bopprfConfig = BopprfFactory.createDefaultConfig(securityModel);
            peqtConfig = PeqtFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setBopprfConfig(BopprfConfig bopprfConfig) {
            this.bopprfConfig = bopprfConfig;
            return this;
        }

        public Builder setPeqtConfig(PeqtConfig peqtConfig) {
            this.peqtConfig = peqtConfig;
            return this;
        }

        @Override
        public Cgs22OpprfPsmConfig build() {
            return new Cgs22OpprfPsmConfig(this);
        }
    }
}
