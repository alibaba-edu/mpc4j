package edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;

/**
 * @author Weiran Liu
 * @date 2022/5/16
 */
public class Gmr21MpPidConfig extends AbstractMultiPartyPtoConfig implements PidConfig {
    /**
     * 多点OPRF协议配置项
     */
    private final MpOprfConfig mpOprfConfig;
    /**
     * PSU协议配置项
     */
    private final PsuConfig psuConfig;

    private Gmr21MpPidConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.psuConfig, builder.mpOprfConfig);
        psuConfig = builder.psuConfig;
        mpOprfConfig = builder.mpOprfConfig;
    }

    @Override
    public PidFactory.PidType getPtoType() {
        return PidFactory.PidType.GMR21_MP;
    }

    public MpOprfConfig getMpOprfConfig() {
        return mpOprfConfig;
    }

    public PsuConfig getPsuConfig() {
        return psuConfig;
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
