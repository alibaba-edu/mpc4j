package edu.alibaba.mpc4j.s2pc.pso.psi.pke.hfh99;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

/**
 * HFH99-byte ecc PSI configure
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class Hfh99ByteEccPsiConfig extends AbstractMultiPartyPtoConfig implements PsiConfig {

    private Hfh99ByteEccPsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.HFH99_BYTE_ECC;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hfh99ByteEccPsiConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Hfh99ByteEccPsiConfig build() {
            return new Hfh99ByteEccPsiConfig(this);
        }
    }
}
