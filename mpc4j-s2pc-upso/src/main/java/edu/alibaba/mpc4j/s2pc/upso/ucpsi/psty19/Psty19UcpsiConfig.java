package edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrFactory;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiFactory.UcpsiType;

/**
 * PSTY19 unbalanced circuit PSI config.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class Psty19UcpsiConfig extends AbstractMultiPartyPtoConfig implements UcpsiConfig {
    /**
     * OKVR config
     */
    private final OkvrConfig okvrConfig;
    /**
     * peqt config
     */
    private final PeqtConfig peqtConfig;

    private Psty19UcpsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.okvrConfig, builder.peqtConfig);
        okvrConfig = builder.okvrConfig;
        peqtConfig = builder.peqtConfig;
    }

    @Override
    public UcpsiType getPtoType() {
        return UcpsiType.PSTY19;
    }

    public OkvrConfig getOkvrConfig() {
        return okvrConfig;
    }

    public PeqtConfig getPeqtConfig() {
        return peqtConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Psty19UcpsiConfig> {
        /**
         * OKVR config
         */
        private OkvrConfig okvrConfig;
        /**
         * peqt config
         */
        private PeqtConfig peqtConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            okvrConfig = OkvrFactory.createDefaultConfig();
            peqtConfig = PeqtFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setOkvrConfig(OkvrConfig okvrConfig) {
            this.okvrConfig = okvrConfig;
            return this;
        }

        public Builder setPeqtConfig(PeqtConfig peqtConfig) {
            this.peqtConfig = peqtConfig;
            return this;
        }

        @Override
        public Psty19UcpsiConfig build() {
            return new Psty19UcpsiConfig(this);
        }
    }
}
