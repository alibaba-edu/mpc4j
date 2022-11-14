package edu.alibaba.mpc4j.s2pc.pso.pid.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidFactory;

/**
 * @author Weiran Liu
 * @date 2022/5/16
 */
public class Gmr21MpPidConfig implements PidConfig {
    /**
     * 多点OPRF协议配置项
     */
    private final MpOprfConfig mpOprfConfig;
    /**
     * PSU协议配置项
     */
    private final PsuConfig psuConfig;

    private Gmr21MpPidConfig(Builder builder) {
        // 协议的环境类型必须相同
        assert builder.psuConfig.getEnvType().equals(builder.mpOprfConfig.getEnvType());
        psuConfig = builder.psuConfig;
        mpOprfConfig = builder.mpOprfConfig;
    }

    @Override
    public PidFactory.PidType getPtoType() {
        return PidFactory.PidType.GMR21_MP;
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

    public MpOprfConfig getMpOprfConfig() {
        return mpOprfConfig;
    }

    public PsuConfig getPsuConfig() {
        return psuConfig;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gmr21MpPidConfig> {
        /**
         * OPRF协议配置项
         */
        private MpOprfConfig mpOprfConfig;
        /**
         * PSU协议配置项
         */
        private PsuConfig psuConfig;

        public Builder() {
            mpOprfConfig = OprfFactory.createMpOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            psuConfig = PsuFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setMpOprfConfig(MpOprfConfig mpOprfConfig) {
            this.mpOprfConfig = mpOprfConfig;
            return this;
        }

        public Builder setPsuConfig(PsuConfig psuConfig) {
            this.psuConfig = psuConfig;
            return this;
        }

        @Override
        public Gmr21MpPidConfig build() {
            return new Gmr21MpPidConfig(this);
        }
    }
}
