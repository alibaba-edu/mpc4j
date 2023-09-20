package edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.PesmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.PesmFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmFactory;

/**
 * CGS22 naive PDSM config.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class Cgs22NaivePdsmConfig extends AbstractMultiPartyPtoConfig implements PdsmConfig {
    /**
     * PESM config
     */
    private final PesmConfig pesmConfig;

    private Cgs22NaivePdsmConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.pesmConfig);
        pesmConfig = builder.pesmConfig;
    }

    public PesmConfig getPesmConfig() {
        return pesmConfig;
    }

    @Override
    public PdsmFactory.PdsmType getPtoType() {
        return PdsmFactory.PdsmType.CGS22_NAIVE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgs22NaivePdsmConfig> {
        /**
         * PESM config
         */
        private PesmConfig pesmConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            pesmConfig = PesmFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setPesmConfig(PesmConfig pesmConfig) {
            this.pesmConfig = pesmConfig;
            return this;
        }

        @Override
        public Cgs22NaivePdsmConfig build() {
            return new Cgs22NaivePdsmConfig(this);
        }
    }
}
