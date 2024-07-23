package edu.alibaba.mpc4j.s2pc.pso.psu.zcl23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory.Gf2kDokvsType;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;

/**
 * ZCL22-SKE-PSU protocol config.
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class Zcl23SkePsuConfig extends AbstractMultiPartyPtoConfig implements PsuConfig {
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
     * GF2K-DOKVS type
     */
    private final Gf2kDokvsType gf2kDokvsType;

    private Zcl23SkePsuConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2cConfig, builder.oprpConfig, builder.coreCotConfig);
        z2cConfig = builder.z2cConfig;
        oprpConfig = builder.oprpConfig;
        coreCotConfig = builder.coreCotConfig;
        gf2kDokvsType = builder.gf2kDokvsType;
    }

    @Override
    public PsuType getPtoType() {
        return PsuType.ZCL23_SKE;
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

    public Gf2kDokvsType getGf2kDokvsType() {
        return gf2kDokvsType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Zcl23SkePsuConfig> {
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
         * GF2E-DOKVS type
         */
        private final Gf2kDokvsType gf2kDokvsType;

        public Builder(SecurityModel securityModel, boolean silent) {
            z2cConfig = Z2cFactory.createDefaultConfig(securityModel, silent);
            oprpConfig = OprpFactory.createDefaultConfig();
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            gf2kDokvsType = Gf2kDokvsType.H3_CLUSTER_BINARY_BLAZE_GCT;
        }

        @Override
        public Zcl23SkePsuConfig build() {
            return new Zcl23SkePsuConfig(this);
        }
    }
}
