package edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnFactory;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtFactory;


/**
 * TCL23 pm-PEQT from Permute Share and mp-OPRF config.
 *
 * @author Liqiang Peng
 * @date 2024/3/5
 */
public class Tcl23PsOprfPmPeqtConfig extends AbstractMultiPartyPtoConfig implements PmPeqtConfig {
    /**
     * OSN config
     */
    private final DosnConfig dosnConfig;
    /**
     * OPRF config
     */
    private final OprfConfig oprfConfig;

    private Tcl23PsOprfPmPeqtConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.dosnConfig, builder.oprfConfig);
        dosnConfig = builder.dosnConfig;
        oprfConfig = builder.oprfConfig;
    }

    @Override
    public PmPeqtFactory.PmPeqtType getPtoType() {
        return PmPeqtFactory.PmPeqtType.TZL23_PS_OPRF;
    }

    public OprfConfig getOprfConfig() {
        return oprfConfig;
    }

    public DosnConfig getOsnConfig() {
        return dosnConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Tcl23PsOprfPmPeqtConfig> {
        /**
         * OSN config
         */
        private DosnConfig dosnConfig;
        /**
         * OPRF config
         */
        private OprfConfig oprfConfig;

        public Builder() {
            dosnConfig = DosnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, false);
            oprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setOsnConfig(DosnConfig dosnConfig) {
            this.dosnConfig = dosnConfig;
            return this;
        }

        public Builder setOprfConfig(OprfConfig oprfConfig) {
            this.oprfConfig = oprfConfig;
            return this;
        }

        @Override
        public Tcl23PsOprfPmPeqtConfig build() {
            return new Tcl23PsOprfPmPeqtConfig(this);
        }
    }
}
