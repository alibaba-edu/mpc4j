package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
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
    /**
     * Z2C config
     */
    private final Z2cConfig z2cConfig;

    private Sj23PeqtUcpsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.peqtConfig);
        peqtConfig = builder.peqtConfig;
        z2cConfig = builder.z2cConfig;
    }

    @Override
    public UcpsiType getPtoType() {
        return UcpsiType.SJ23_PEQT;
    }

    public PeqtConfig getPeqtConfig() {
        return peqtConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Sj23PeqtUcpsiConfig> {
        /**
         * peqt config
         */
        private PeqtConfig peqtConfig;
        /**
         * Z2C config
         */
        private Z2cConfig z2cConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            peqtConfig = PeqtFactory.createDefaultConfig(securityModel, silent);
            z2cConfig = Z2cFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setPeqtConfig(PeqtConfig peqtConfig) {
            this.peqtConfig = peqtConfig;
            return this;
        }

        public Builder setZ2cConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
            return this;
        }

        @Override
        public Sj23PeqtUcpsiConfig build() {
            return new Sj23PeqtUcpsiConfig(this);
        }
    }
}
