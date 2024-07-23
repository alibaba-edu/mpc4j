package edu.alibaba.mpc4j.work.dpsi.ccpsi;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.ldp.nominal.binary.BinaryLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.binary.BinaryLdpFactory;
import edu.alibaba.mpc4j.work.dpsi.DpsiConfig;
import edu.alibaba.mpc4j.work.dpsi.DpsiFactory.DpPsiType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;

/**
 * DPSI based on client-payload circuit PSI config.
 *
 * @author Yufei Wang, Weiran Liu
 * @date 2023/8/15
 */
public class CcpsiDpsiConfig extends AbstractMultiPartyPtoConfig implements DpsiConfig {
    /**
     * ε
     */
    private final double epsilon;
    /**
     * CCPSI
     */
    private final CcpsiConfig ccpsiConfig;
    /**
     * Binary LDP
     */
    private final BinaryLdpConfig binaryLdpConfig;

    private CcpsiDpsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.ccpsiConfig);
        epsilon = builder.epsilon;
        ccpsiConfig = builder.ccpsiConfig;
        binaryLdpConfig = builder.binaryLdpConfig;
    }

    @Override
    public DpPsiType getPtoType() {
        return DpPsiType.CCPSI_BASED;
    }

    @Override
    public double getEpsilon() {
        return epsilon;
    }

    public CcpsiConfig getCcpsiConfig() {
        return ccpsiConfig;
    }

    public BinaryLdpConfig getBinaryLdpConfig() {
        return binaryLdpConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<CcpsiDpsiConfig> {
        /**
         * ε
         */
        private final double epsilon;
        /**
         * binary LDP config
         */
        private final BinaryLdpConfig binaryLdpConfig;
        /**
         * CCPSI config
         */
        private CcpsiConfig ccpsiConfig;

        public Builder(double epsilon) {
            MathPreconditions.checkPositive("ε", epsilon);
            this.epsilon = epsilon;
            ccpsiConfig = CcpsiFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
            binaryLdpConfig = BinaryLdpFactory.createDefaultConfig(epsilon);
        }

        public Builder setCcpsiConfig(CcpsiConfig ccpsiConfig) {
            this.ccpsiConfig = ccpsiConfig;
            return this;
        }

        @Override
        public CcpsiDpsiConfig build() {
            return new CcpsiDpsiConfig(this);
        }
    }
}
