package edu.alibaba.mpc4j.s2pc.pso.pid.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidFactory;

/**
 * GMR21宽松PID协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/12
 */
public class Gmr21SloppyPidConfig implements PidConfig {
    /**
     * OPRF协议配置项
     */
    private final OprfConfig oprfConfig;
    /**
     * PSU协议配置项
     */
    private final PsuConfig psuConfig;
    /**
     * Sloppy的OKVS类型
     */
    private final OkvsType sloppyOkvsType;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Gmr21SloppyPidConfig(Builder builder) {
        // 协议的环境类型必须相同
        assert builder.psuConfig.getEnvType().equals(builder.oprfConfig.getEnvType());
        psuConfig = builder.psuConfig;
        oprfConfig = builder.oprfConfig;
        sloppyOkvsType = builder.sloppyOkvsType;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public PidFactory.PidType getPtoType() {
        return PidFactory.PidType.GMR21_SLOPPY;
    }

    @Override
    public void setEnvType(EnvType envType) {
        oprfConfig.setEnvType(envType);
        psuConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return oprfConfig.getEnvType();
    }

    public OprfConfig getOprfConfig() {
        return oprfConfig;
    }

    public PsuConfig getPsuConfig() {
        return psuConfig;
    }

    public OkvsType getSloppyOkvsType() {
        return sloppyOkvsType;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (oprfConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = oprfConfig.getSecurityModel();
        }
        if (psuConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = psuConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gmr21SloppyPidConfig> {
        /**
         * OPRF协议配置项
         */
        private OprfConfig oprfConfig;
        /**
         * PSU协议配置项
         */
        private PsuConfig psuConfig;
        /**
         * OKVS类型
         */
        private OkvsType sloppyOkvsType;
        /**
         * 布谷鸟哈希类型
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder() {
            oprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            psuConfig = PsuFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            sloppyOkvsType = OkvsType.MEGA_BIN;
            cuckooHashBinType = CuckooHashBinType.NAIVE_3_HASH;
        }

        public Builder setOprfConfig(OprfConfig oprfConfig) {
            this.oprfConfig = oprfConfig;
            return this;
        }

        public Builder setPsuConfig(PsuConfig psuConfig) {
            this.psuConfig = psuConfig;
            return this;
        }

        public Builder setSloppyOkvsType(OkvsType sloppyOkvsType) {
            this.sloppyOkvsType = sloppyOkvsType;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Gmr21SloppyPidConfig build() {
            return new Gmr21SloppyPidConfig(this);
        }
    }
}
