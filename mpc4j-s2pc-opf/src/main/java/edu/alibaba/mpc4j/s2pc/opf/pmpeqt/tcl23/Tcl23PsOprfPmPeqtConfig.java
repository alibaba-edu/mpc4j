package edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
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
    private final OsnConfig osnConfig;
    /**
     * OPRF config
     */
    private final OprfConfig oprfConfig;

    private Tcl23PsOprfPmPeqtConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.osnConfig, builder.oprfConfig);
        osnConfig = builder.osnConfig;
        oprfConfig = builder.oprfConfig;
    }

    @Override
    public PmPeqtFactory.PmPeqtType getPtoType() {
        return PmPeqtFactory.PmPeqtType.TZL23_PS_OPRF;
    }

    public OprfConfig getOprfConfig() {
        return oprfConfig;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Tcl23PsOprfPmPeqtConfig> {
        /**
         * OSN config
         */
        private OsnConfig osnConfig;
        /**
         * OPRF config
         */
        private OprfConfig oprfConfig;

        public Builder(boolean silent) {
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            oprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setOsnConfig(OsnConfig osnConfig) {
            this.osnConfig = osnConfig;
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
