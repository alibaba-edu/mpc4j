package edu.alibaba.mpc4j.s2pc.pso.psica.ccpsi;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.HammingConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.HammingFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaFactory;

/**
 * client-payload circuit-PSI Cardinality config.
 *
 * @author Qixian Zhou
 * @date 2023/4/24
 */
public class CcPsiCaConfig extends AbstractMultiPartyPtoConfig implements PsiCaConfig {
    /**
     * client-payload circuit PSI config
     */
    private final CcpsiConfig ccpsiConfig;
    /**
     * hamming config
     */
    private final HammingConfig hammingConfig;

    private CcPsiCaConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.ccpsiConfig, builder.hammingConfig);
        this.ccpsiConfig = builder.ccpsiConfig;
        this.hammingConfig = builder.hammingConfig;
    }

    public CcpsiConfig getCcpsiConfig() {
        return ccpsiConfig;
    }

    public HammingConfig getHammingConfig() {
        return hammingConfig;
    }

    @Override
    public PsiCaFactory.PsiCaType getPtoType() {
        return PsiCaFactory.PsiCaType.CCPSI;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<CcPsiCaConfig> {
        /**
         * client-payload circuit PSI config
         */
        private CcpsiConfig ccpsiConfig;
        /**
         * hamming config
         */
        private HammingConfig hammingConfig;

        public Builder(boolean silent) {
            ccpsiConfig = CcpsiFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            hammingConfig = HammingFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        public Builder setCcpsiConfig(CcpsiConfig ccpsiConfig) {
            this.ccpsiConfig = ccpsiConfig;
            return this;
        }

        public Builder setHammingConfig(HammingConfig hammingConfig) {
            this.hammingConfig = hammingConfig;
            return this;
        }

        @Override
        public CcPsiCaConfig build() {
            return new CcPsiCaConfig(this);
        }
    }
}
