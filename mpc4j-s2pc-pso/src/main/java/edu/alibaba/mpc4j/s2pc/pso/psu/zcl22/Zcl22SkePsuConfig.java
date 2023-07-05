package edu.alibaba.mpc4j.s2pc.pso.psu.zcl22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.crypto.matrix.okve.ovdm.gf2e.Gf2eOvdmFactory.Gf2eOvdmType;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;

/**
 * ZCL22-SKE-PSU协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class Zcl22SkePsuConfig extends AbstractMultiPartyPtoConfig implements PsuConfig {
    /**
     * Z2 circuit config
     */
    private final Z2cConfig z2cConfig;
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
        super(SecurityModel.SEMI_HONEST, builder.z2cConfig, builder.oprpConfig, builder.coreCotConfig);
        z2cConfig = builder.z2cConfig;
        oprpConfig = builder.oprpConfig;
        coreCotConfig = builder.coreCotConfig;
        gf2eOvdmType = builder.gf2eOvdmType;
    }

    @Override
    public PsuType getPtoType() {
        return PsuType.ZCL22_SKE;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Zcl22SkePsuConfig> {
        /**
         * Z2 circuit config
         */
        private Z2cConfig z2cConfig;
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

        public Builder(SecurityModel securityModel) {
            z2cConfig = Z2cFactory.createDefaultConfig(securityModel, true);
            oprpConfig = OprpFactory.createDefaultConfig(securityModel, true);
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            gf2eOvdmType = Gf2eOvdmType.H3_SINGLETON_GCT;
        }

        public Builder setBcConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
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
