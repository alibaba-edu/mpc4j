package edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmFactory;

/**
 * CGS22 OPPRF-based PSM config.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class Cgs22OpprfPdsmConfig extends AbstractMultiPartyPtoConfig implements PdsmConfig {
    /**
     * batched OPPRF config
     */
    private final BopprfConfig bopprfConfig;
    /**
     * PEQT config
     */
    private final PeqtConfig peqtConfig;

    private Cgs22OpprfPdsmConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.bopprfConfig, builder.peqtConfig);
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
    public PdsmFactory.PdsmType getPtoType() {
        return PdsmFactory.PdsmType.CGS22_OPPRF;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgs22OpprfPdsmConfig> {
        /**
         * batched OPPRF config
         */
        private BopprfConfig bopprfConfig;
        /**
         * PEQT config
         */
        private PeqtConfig peqtConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            bopprfConfig = BopprfFactory.createDefaultConfig();
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
        public Cgs22OpprfPdsmConfig build() {
            return new Cgs22OpprfPdsmConfig(this);
        }
    }
}
