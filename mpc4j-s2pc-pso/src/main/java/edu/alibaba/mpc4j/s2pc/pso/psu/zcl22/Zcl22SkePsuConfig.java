package edu.alibaba.mpc4j.s2pc.pso.psu.zcl22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.gf2e.Gf2eOvdmFactory.Gf2eOvdmType;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprp.OprpConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprp.OprpFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;

/**
 * ZCL22-SKE-PSU协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class Zcl22SkePsuConfig implements PsuConfig {
    /**
     * BC协议配置项
     */
    private final BcConfig bcConfig;
    /**
     * OPRP协议配置项
     */
    private final OprpConfig oprpConfig;
    /**
     * 核COT协议配置项
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * GF2E-OVDM类型
     */
    private final Gf2eOvdmType gf2eOvdmType;

    private Zcl22SkePsuConfig(Builder builder) {
        // 协议的环境类型必须相同
        assert builder.bcConfig.getEnvType().equals(builder.oprpConfig.getEnvType());
        assert builder.bcConfig.getEnvType().equals(builder.coreCotConfig.getEnvType());
        bcConfig = builder.bcConfig;
        oprpConfig = builder.oprpConfig;
        coreCotConfig = builder.coreCotConfig;
        gf2eOvdmType = builder.gf2eOvdmType;
    }

    @Override
    public PsuType getPtoType() {
        return PsuType.ZCL22_SKE;
    }

    public BcConfig getBcConfig() {
        return bcConfig;
    }

    public OprpConfig getOprpConfig() {
        return oprpConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public Gf2eOvdmType getGf2eOvdmType() {
        return gf2eOvdmType;
    }

    @Override
    public void setEnvType(EnvType envType) {
        bcConfig.setEnvType(envType);
        oprpConfig.setEnvType(envType);
        coreCotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return bcConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (bcConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = bcConfig.getSecurityModel();
        }
        if (oprpConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = oprpConfig.getSecurityModel();
        }
        if (coreCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = coreCotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Zcl22SkePsuConfig> {
        /**
         * BC协议配置项
         */
        private BcConfig bcConfig;
        /**
         * OPRP协议配置项
         */
        private OprpConfig oprpConfig;
        /**
         * 核COT协议配置项
         */
        private CoreCotConfig coreCotConfig;
        /**
         * GF2x-OVDM类型
         */
        private Gf2eOvdmType gf2eOvdmType;

        public Builder() {
            bcConfig = BcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            oprpConfig = OprpFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            gf2eOvdmType = Gf2eOvdmType.H3_SINGLETON_GCT;
        }

        public Builder setBcConfig(BcConfig bcConfig) {
            this.bcConfig = bcConfig;
            return this;
        }

        public Builder setOprpConfig(OprpConfig oprpConfig) {
            this.oprpConfig = oprpConfig;
            return this;
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setGf2eOvdmType(Gf2eOvdmType gf2eOvdmType) {
            this.gf2eOvdmType = gf2eOvdmType;
            return this;
        }

        @Override
        public Zcl22SkePsuConfig build() {
            return new Zcl22SkePsuConfig(this);
        }
    }
}
