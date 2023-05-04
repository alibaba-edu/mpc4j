package edu.alibaba.mpc4j.s2pc.upso.ucpsi.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmFactory;
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
public class Cgs22UcpsiConfig implements UcpsiConfig {
    /**
     * unbalanced related batch OPPRF config
     */
    private final UrbopprfConfig urbopprfConfig;
    /**
     * private set membership config
     */
    private final PsmConfig psmConfig;

    private Cgs22UcpsiConfig(Builder builder) {
        assert builder.urbopprfConfig.getEnvType().equals(builder.psmConfig.getEnvType());
        urbopprfConfig = builder.urbopprfConfig;
        psmConfig = builder.psmConfig;
    }

    @Override
    public UcpsiType getPtoType() {
        return UcpsiType.CGS22;
    }

    @Override
    public void setEnvType(EnvType envType) {
        urbopprfConfig.setEnvType(envType);
        psmConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return urbopprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public UrbopprfConfig getUrbopprfConfig() {
        return urbopprfConfig;
    }

    public PsmConfig getPsmConfig() {
        return psmConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgs22UcpsiConfig> {
        /**
         * unbalanced related batch OPPRF config
         */
        private UrbopprfConfig urbopprfConfig;
        /**
         * private set membership config
         */
        private PsmConfig psmConfig;

        public Builder(boolean silent) {
            urbopprfConfig = UrbopprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            psmConfig = PsmFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        public Builder setUrbopprfConfig(UrbopprfConfig urbopprfConfig) {
            this.urbopprfConfig = urbopprfConfig;
            return this;
        }

        public Builder setPsmConfig(PsmConfig psmConfig) {
            this.psmConfig = psmConfig;
            return this;
        }

        @Override
        public Cgs22UcpsiConfig build() {
            return new Cgs22UcpsiConfig(this);
        }
    }
}
