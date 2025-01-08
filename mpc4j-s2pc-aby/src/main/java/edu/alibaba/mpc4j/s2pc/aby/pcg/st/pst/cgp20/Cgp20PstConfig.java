package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.cgp20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.cgp20.Cgp20BstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstFactory.PstType;

/**
 * Partial share translate config, using the fixed Cgp20Bst
 *
 * @author Feng Han
 * @date 2024/8/6
 */
public class Cgp20PstConfig extends AbstractMultiPartyPtoConfig implements PstConfig {
    /**
     * BP-CDPPRF
     */
    private final Cgp20BstConfig bstConfig;

    private Cgp20PstConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.bstConfig);
        bstConfig = builder.bstConfig;
    }

    @Override
    public Cgp20BstConfig getBstConfig() {
        return bstConfig;
    }

    @Override
    public PstType getPtoType() {
        return PstType.CGP20;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgp20PstConfig> {
        /**
         * BP-CDPPRF
         */
        private Cgp20BstConfig bstConfig;

        public Builder(boolean silent) {
            bstConfig = new Cgp20BstConfig.Builder().build();
        }

        public Builder setBstConfig(Cgp20BstConfig bstConfig){
            this.bstConfig = bstConfig;
            return this;
        }

        @Override
        public Cgp20PstConfig build() {
            return new Cgp20PstConfig(this);
        }
    }
}
