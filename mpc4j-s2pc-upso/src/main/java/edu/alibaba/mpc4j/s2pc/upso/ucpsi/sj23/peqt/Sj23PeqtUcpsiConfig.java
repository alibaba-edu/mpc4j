package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiConfig;

import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiFactory.UcpsiType;

/**
 * SJ23 peqt unbalanced circuit PSI config.
 *
 * @author Liqiang Peng
 * @date 2023/7/17
 */
public class Sj23PeqtUcpsiConfig extends AbstractMultiPartyPtoConfig implements UcpsiConfig {
    /**
     * peqt config
     */
    private final PeqtConfig peqtConfig;

    private Sj23PeqtUcpsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.peqtConfig);
        peqtConfig = builder.peqtConfig;
    }

    @Override
    public UcpsiType getPtoType() {
        return UcpsiType.SJ23_PEQT;
    }

    public PeqtConfig getPeqtConfig() {
        return peqtConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Sj23PeqtUcpsiConfig> {
        /**
         * peqt config
         */
        private PeqtConfig peqtConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            peqtConfig = PeqtFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setPeqtConfig(PeqtConfig peqtConfig) {
            this.peqtConfig = peqtConfig;
            return this;
        }

        @Override
        public Sj23PeqtUcpsiConfig build() {
            return new Sj23PeqtUcpsiConfig(this);
        }
    }
}
