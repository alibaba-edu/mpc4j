package edu.alibaba.mpc4j.s2pc.pso.pmid.zcl22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidConfig;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidFactory.PmidType;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;

/**
 * ZCL22多点PMID协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/10
 */
public class Zcl22MpPmidConfig implements PmidConfig {
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
        // 协议的环境类型必须相同
        assert builder.mpOprfConfig.getEnvType().equals(builder.psuConfig.getEnvType());
        mpOprfConfig = builder.mpOprfConfig;
        psuConfig = builder.psuConfig;
        sigmaOkvsType = builder.sigmaOkvsType;
    }

    @Override
    public PmidType getPtoType() {
        return PmidType.ZCL22_MP;
    }

    @Override
    public void setEnvType(EnvType envType) {
        mpOprfConfig.setEnvType(envType);
        psuConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return mpOprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (mpOprfConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = mpOprfConfig.getSecurityModel();
        }
        if (psuConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = psuConfig.getSecurityModel();
        }
        return securityModel;
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
