package edu.alibaba.mpc4j.s2pc.pso.psica.hfh99;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaFactory;

/**
 * ECC-based HFH99 PSI Cardinality config.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class Hfh99EccPsiCaConfig extends AbstractMultiPartyPtoConfig implements PsiCaConfig {
    /**
     * compress encode
     */
    private final boolean compressEncode;

    private Hfh99EccPsiCaConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        this.compressEncode = builder.compressEncode;
    }

    @Override
    public PsiCaFactory.PsiCaType getPtoType() {
        return PsiCaFactory.PsiCaType.HFH99_ECC;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hfh99EccPsiCaConfig> {
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
        public Hfh99EccPsiCaConfig build() {
            return new Hfh99EccPsiCaConfig(this);
        }
    }
}
