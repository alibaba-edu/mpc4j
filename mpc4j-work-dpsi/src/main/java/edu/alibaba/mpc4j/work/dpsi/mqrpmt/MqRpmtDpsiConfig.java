package edu.alibaba.mpc4j.work.dpsi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.ldp.nominal.binary.BinaryLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.binary.BinaryLdpFactory;

import edu.alibaba.mpc4j.work.dpsi.DpsiConfig;
import edu.alibaba.mpc4j.work.dpsi.DpsiFactory.DpPsiType;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;

/**
 * DPSI based on mqRPMT config.
 *
 * @author Yufei Wang, Weiran Liu
 * @date 2023/9/19
 */
public class MqRpmtDpsiConfig extends AbstractMultiPartyPtoConfig implements DpsiConfig {
    /**
     * ε_c
     */
    private final double epsilon;
    /**
     * ε_I
     */
    private final double psicaEpsilon;
    /**
     * ε_D
     */
    private final double psdcaEpsilon;
    /**
     * δ
     */
    private final double delta;
    /**
     * max PSI-CA dummy size
     */
    private final int maxPsicaDummySize;
    /**
     * max PSD-CA dummy size
     */
    private final int maxPsdcaDummySize;
    /**
     * binary LDP
     */
    private final BinaryLdpConfig binaryLdpConfig;
    /**
     * mqRPMT
     */
    private final MqRpmtConfig mqRpmtConfig;

    private MqRpmtDpsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.mqRpmtConfig);
        epsilon = builder.outputEpsilon;
        delta = builder.delta;
        this.psicaEpsilon = builder.psicaEpsilon;
        this.psdcaEpsilon = builder.psdcaEpsilon;
        this.maxPsicaDummySize = builder.maxPsicaDummySize;
        this.maxPsdcaDummySize = builder.maxPsdcaDummySize;
        binaryLdpConfig = builder.binaryLdpConfig;
        mqRpmtConfig = builder.mqRpmtConfig;
    }

    @Override
    public DpPsiType getPtoType() {
        return DpPsiType.MQ_RPMT_BASED;
    }

    @Override
    public double getEpsilon() {
        return epsilon;
    }

    public BinaryLdpConfig getBinaryLdpConfig() {
        return binaryLdpConfig;
    }

    public double getPsicaEpsilon() {
        return psicaEpsilon;
    }

    public double getPsdcaEpsilon() {
        return psdcaEpsilon;
    }

    public double getDelta() {
        return delta;
    }

    public int getMaxPsicaDummySize() {
        return maxPsicaDummySize;
    }

    public int getMaxPsdcaDummySize() {
        return maxPsdcaDummySize;
    }

    public MqRpmtConfig getMqRpmtConfig() {
        return mqRpmtConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<MqRpmtDpsiConfig> {
        /**
         * ε_c
         */
        private final double outputEpsilon;
        /**
         * ε_I
         */
        private final double psicaEpsilon;
        /**
         * ε_D
         */
        private final double psdcaEpsilon;
        /**
         * δ
         */
        private double delta;
        /**
         * max PSI-CA dummy size
         */
        private int maxPsicaDummySize;
        /**
         * max PSD-CA dummy size
         */
        private int maxPsdcaDummySize;
        /**
         * binary LDP
         */
        private final BinaryLdpConfig binaryLdpConfig;
        /**
         * mqRPMT
         */
        private MqRpmtConfig mqRpmtConfig;

        public Builder(double outputEpsilon, double psicaEpsilon, double psdcaEpsilon) {
            MathPreconditions.checkPositive("ε_c", outputEpsilon);
            this.outputEpsilon = outputEpsilon;
            binaryLdpConfig = BinaryLdpFactory.createDefaultConfig(outputEpsilon);
            this.delta = 0.00001;
            MathPreconditions.checkPositive("ε_psica", psicaEpsilon);
            this.psicaEpsilon = psicaEpsilon;
            maxPsicaDummySize = MqRpmtDpUtils.getMaxDummySize(psicaEpsilon, delta);
            MathPreconditions.checkPositive("ε_psdca", psdcaEpsilon);
            this.psdcaEpsilon = psdcaEpsilon;
            maxPsdcaDummySize = MqRpmtDpUtils.getMaxDummySize(psdcaEpsilon, delta);
            mqRpmtConfig = MqRpmtFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setMqRpmtConfig(MqRpmtConfig mqRpmtConfig) {
            this.mqRpmtConfig = mqRpmtConfig;
            return this;
        }

        public Builder setDelta(double delta) {
            MathPreconditions.checkPositive("delta", delta);
            this.delta = delta;
            maxPsicaDummySize = MqRpmtDpUtils.getMaxDummySize(psicaEpsilon, delta);
            maxPsdcaDummySize = MqRpmtDpUtils.getMaxDummySize(psdcaEpsilon, delta);
            return this;
        }

        @Override
        public MqRpmtDpsiConfig build() {
            return new MqRpmtDpsiConfig(this);
        }
    }
}