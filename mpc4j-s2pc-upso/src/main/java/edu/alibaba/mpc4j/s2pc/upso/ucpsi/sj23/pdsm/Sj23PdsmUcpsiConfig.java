package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pdsm;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmFactory;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiConfig;

import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiFactory.UcpsiType;

/**
 * SJ23 pmt unbalanced circuit PSI config.
 *
 * @author Liqiang Peng
 * @date 2023/7/17
 */
public class Sj23PdsmUcpsiConfig extends AbstractMultiPartyPtoConfig implements UcpsiConfig {
    /**
     * pmt config
     */
    private final PdsmConfig pdsmConfig;

    private Sj23PdsmUcpsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.pdsmConfig);
        pdsmConfig = builder.pdsmConfig;
    }

    @Override
    public UcpsiType getPtoType() {
        return UcpsiType.SJ23_PDSM;
    }

    public PdsmConfig getPsmConfig() {
        return pdsmConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Sj23PdsmUcpsiConfig> {
        /**
         * pmt config
         */
        private PdsmConfig pdsmConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            pdsmConfig = PdsmFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setPsmConfig(PdsmConfig pdsmConfig) {
            this.pdsmConfig = pdsmConfig;
            return this;
        }

        @Override
        public Sj23PdsmUcpsiConfig build() {
            return new Sj23PdsmUcpsiConfig(this);
        }
    }
}
