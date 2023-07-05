package edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.UbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.UbopprfFactory;

import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiFactory.*;

/**
 * PSTY19 unbalanced circuit PSI config.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class Psty19UcpsiConfig extends AbstractMultiPartyPtoConfig implements UcpsiConfig {
    /**
     * unbalanced batch OPPRF config
     */
    private final UbopprfConfig ubopprfConfig;
    /**
     * peqt config
     */
    private final PeqtConfig peqtConfig;

    private Psty19UcpsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.ubopprfConfig, builder.peqtConfig);
        ubopprfConfig = builder.ubopprfConfig;
        peqtConfig = builder.peqtConfig;
    }

    @Override
    public UcpsiType getPtoType() {
        return UcpsiType.PSTY19;
    }

    public UbopprfConfig getUbopprfConfig() {
        return ubopprfConfig;
    }

    public PeqtConfig getPeqtConfig() {
        return peqtConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Psty19UcpsiConfig> {
        /**
         * unbalanced batch OPPRF config
         */
        private UbopprfConfig ubopprfConfig;
        /**
         * peqt config
         */
        private PeqtConfig peqtConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            ubopprfConfig = UbopprfFactory.createDefaultConfig();
            peqtConfig = PeqtFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setUbopprfConfig(UbopprfConfig ubopprfConfig) {
            this.ubopprfConfig = ubopprfConfig;
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
