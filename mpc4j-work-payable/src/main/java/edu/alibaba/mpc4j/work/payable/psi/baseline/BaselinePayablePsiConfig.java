package edu.alibaba.mpc4j.work.payable.psi.baseline;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rr22.Rr22PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.gmr21.Gmr21PsiCaConfig;
import edu.alibaba.mpc4j.work.payable.psi.PayablePsiConfig;
import edu.alibaba.mpc4j.work.payable.psi.PayablePsiFactory;

/**
 * Baseline payable PSI config.
 *
 * @author Liqiang Peng
 * @date 2024/7/1
 */
public class BaselinePayablePsiConfig extends AbstractMultiPartyPtoConfig implements PayablePsiConfig {

    /**
     * PSI-CA config
     */
    private final PsiCaConfig psiCaConfig;
    /**
     * PSI config
     */
    private final PsiConfig psiConfig;

    public BaselinePayablePsiConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.psiCaConfig, builder.psiConfig);
        this.psiCaConfig = builder.psiCaConfig;
        this.psiConfig = builder.psiConfig;
    }

    @Override
    public PayablePsiFactory.PayablePsiType getPtoType() {
        return PayablePsiFactory.PayablePsiType.BASELINE;
    }

    public PsiCaConfig getPsiCaConfig() {
        return psiCaConfig;
    }

    public PsiConfig getPsiConfig() {
        return psiConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<BaselinePayablePsiConfig> {
        /**
         * PSI-CA config
         */
        private PsiCaConfig psiCaConfig;
        /**
         * PSI config
         */
        private PsiConfig psiConfig;

        public Builder() {
            psiCaConfig = new Gmr21PsiCaConfig.Builder(false).build();
            psiConfig = new Rr22PsiConfig.Builder(SecurityModel.MALICIOUS).build();
        }

        public Builder setPsiCaConfig(PsiCaConfig psiCaConfig) {
            this.psiCaConfig = psiCaConfig;
            return this;
        }

        public Builder setPsiConfig(PsiConfig psiConfig) {
            this.psiConfig = psiConfig;
            return this;
        }

        @Override
        public BaselinePayablePsiConfig build() {
            return new BaselinePayablePsiConfig(this);
        }
    }
}