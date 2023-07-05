package edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.crypto.matrix.okve.okvs.OkvsFactory.OkvsType;
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
    private final OkvsType sigmaOkvsType;

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

    public OkvsType getSigmaOkvsType() {
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
        private OkvsType sigmaOkvsType;

        public Builder() {
            super();
            mpOprfConfig = OprfFactory.createMpOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            psuConfig = PsuFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            sigmaOkvsType = OkvsType.H3_SINGLETON_GCT;
        }

        public Builder setMpOprfConfig(MpOprfConfig mpOprfConfig) {
            this.mpOprfConfig = mpOprfConfig;
            return this;
        }

        public Builder setPsuConfig(PsuConfig psuConfig) {
            this.psuConfig = psuConfig;
            return this;
        }

        public Builder setSigmaOkvsType(OkvsType sigmaOkvsType) {
            this.sigmaOkvsType = sigmaOkvsType;
            return this;
        }

        @Override
        public Zcl22MpPmidConfig build() {
            return new Zcl22MpPmidConfig(this);
        }
    }
}
