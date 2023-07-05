package edu.alibaba.mpc4j.s2pc.pso.psica.cgt12;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaFactory;

/**
 * ECC-based CGT12 PSI Cardinality config.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class Cgt12EccPsiCaConfig extends AbstractMultiPartyPtoConfig implements PsiCaConfig {
    /**
     * compress encode
     */
    private final boolean compressEncode;

    private Cgt12EccPsiCaConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        compressEncode = builder.compressEncode;
    }

    @Override
    public PsiCaFactory.PsiCaType getPtoType() {
        return PsiCaFactory.PsiCaType.CGT12_ECC;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgt12EccPsiCaConfig> {
        /**
         * compress encode
         */
        private boolean compressEncode;

        public Builder() {
            compressEncode = true;
        }

        public Builder setCompressEncode(boolean compressEncode) {
            this.compressEncode = compressEncode;
            return this;
        }

        @Override
        public Cgt12EccPsiCaConfig build() {
            return new Cgt12EccPsiCaConfig(this);
        }
    }
}

