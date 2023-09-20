package edu.alibaba.mpc4j.s2pc.upso.ucpsi.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmFactory;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.UrbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.UrbopprfFactory;

import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiFactory.UcpsiType;

/**
 * CGS22 unbalanced circuit PSI config.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class Cgs22UcpsiConfig extends AbstractMultiPartyPtoConfig implements UcpsiConfig {
    /**
     * unbalanced related batch OPPRF config
     */
    private final UrbopprfConfig urbopprfConfig;
    /**
     * private set membership config
     */
    private final PdsmConfig pdsmConfig;

    private Cgs22UcpsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.urbopprfConfig, builder.pdsmConfig);
        urbopprfConfig = builder.urbopprfConfig;
        pdsmConfig = builder.pdsmConfig;
    }

    @Override
    public UcpsiType getPtoType() {
        return UcpsiType.CGS22;
    }

    public UrbopprfConfig getUrbopprfConfig() {
        return urbopprfConfig;
    }

    public PdsmConfig getPsmConfig() {
        return pdsmConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgs22UcpsiConfig> {
        /**
         * unbalanced related batch OPPRF config
         */
        private UrbopprfConfig urbopprfConfig;
        /**
         * private set membership config
         */
        private PdsmConfig pdsmConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            urbopprfConfig = UrbopprfFactory.createDefaultConfig();
            pdsmConfig = PdsmFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setUrbopprfConfig(UrbopprfConfig urbopprfConfig) {
            this.urbopprfConfig = urbopprfConfig;
            return this;
        }

        public Builder setPsmConfig(PdsmConfig pdsmConfig) {
            this.pdsmConfig = pdsmConfig;
            return this;
        }

        @Override
        public Cgs22UcpsiConfig build() {
            return new Cgs22UcpsiConfig(this);
        }
    }
}
