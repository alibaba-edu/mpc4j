package edu.alibaba.mpc4j.s2pc.pso.psi.pke.hfh99;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

/**
 * HFH99-ecc PSI configure
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class Hfh99EccPsiConfig extends AbstractMultiPartyPtoConfig implements PsiConfig {
    /**
     * compress encode?
     */
    private final boolean compressEncode;

    private Hfh99EccPsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        compressEncode = builder.compressEncode;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.HFH99_ECC;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hfh99EccPsiConfig> {
        /**
         * compress encode?
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
        public Hfh99EccPsiConfig build() {
            return new Hfh99EccPsiConfig(this);
        }
    }
}
