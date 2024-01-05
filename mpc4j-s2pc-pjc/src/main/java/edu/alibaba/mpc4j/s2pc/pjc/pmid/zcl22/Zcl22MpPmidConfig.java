package edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidFactory.PmidType;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;

/**
 * ZCL22多点PMID协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/10
 */
public class Zcl22MpPmidConfig extends AbstractMultiPartyPtoConfig implements PmidConfig {
    /**
     * MP-OPRF协议配置项
     */
    private final MpOprfConfig mpOprfConfig;
    /**
     * PSU协议配置项
     */
    private final PsuConfig psuConfig;
    /**
     * σ的OKVS类型
     */
    private final Gf2eDokvsType sigmaOkvsType;

    private Zcl22MpPmidConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.mpOprfConfig, builder.psuConfig);
        mpOprfConfig = builder.mpOprfConfig;
        psuConfig = builder.psuConfig;
        sigmaOkvsType = builder.sigmaOkvsType;
    }

    @Override
    public PmidType getPtoType() {
        return PmidType.ZCL22_MP;
    }

    public MpOprfConfig getMpOprfConfig() {
        return mpOprfConfig;
    }

    public PsuConfig getPsuConfig() {
        return psuConfig;
    }

    public Gf2eDokvsType getSigmaOkvsType() {
        return sigmaOkvsType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Zcl22MpPmidConfig> {
        /**
         * MP-OPRF协议配置项
         */
        private MpOprfConfig mpOprfConfig;
        /**
         * PSU协议配置项
         */
        private PsuConfig psuConfig;
        /**
         * σ的OKVS类型
         */
        private Gf2eDokvsType sigmaOkvsType;

        public Builder() {
            super();
            mpOprfConfig = OprfFactory.createMpOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            psuConfig = PsuFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            sigmaOkvsType = Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT;
        }

        public Builder setMpOprfConfig(MpOprfConfig mpOprfConfig) {
            this.mpOprfConfig = mpOprfConfig;
            return this;
        }

        public Builder setPsuConfig(PsuConfig psuConfig) {
            this.psuConfig = psuConfig;
            return this;
        }

        public Builder setSigmaOkvsType(Gf2eDokvsType sigmaOkvsType) {
            this.sigmaOkvsType = sigmaOkvsType;
            return this;
        }

        @Override
        public Zcl22MpPmidConfig build() {
            return new Zcl22MpPmidConfig(this);
        }
    }
}
