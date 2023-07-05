package edu.alibaba.mpc4j.s2pc.pso.psu.krtw19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.crypto.matrix.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;

/**
 * KRTW19-ORI-PSU协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
public class Krtw19OriPsuConfig extends AbstractMultiPartyPtoConfig implements PsuConfig {
    /**
     * RPMT所用OPRF配置项
     */
    private final OprfConfig rpmtOprfConfig;
    /**
     * PEQT所用OPRF配置项
     */
    private final OprfConfig peqtOprfConfig;
    /**
     * 核COT协议配置项
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * OKVS类型
     */
    private final OkvsType okvsType;
    /**
     * 流水线数量
     */
    private final int pipeSize;

    private Krtw19OriPsuConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.rpmtOprfConfig, builder.peqtOprfConfig, builder.coreCotConfig);
        rpmtOprfConfig = builder.rpmtOprfConfig;
        peqtOprfConfig = builder.peqtOprfConfig;
        coreCotConfig = builder.coreCotConfig;
        okvsType = builder.okvsType;
        pipeSize = builder.pipeSize;
    }

    @Override
    public PsuFactory.PsuType getPtoType() {
        return PsuFactory.PsuType.KRTW19_ORI;
    }

    public OprfConfig getRpmtOprfConfig() {
        return rpmtOprfConfig;
    }

    public OprfConfig getPeqtOprfConfig() {
        return peqtOprfConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public OkvsType getOkvsType() {
        return okvsType;
    }

    public int getPipeSize() {
        return pipeSize;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Krtw19OriPsuConfig> {
        /**
         * RPMT所用OPRF配置项
         */
        private OprfConfig rpmtOprfConfig;
        /**
         * PEQT所用OPRF配置项
         */
        private OprfConfig peqtOprfConfig;
        /**
         * 核COT协议配置项
         */
        private CoreCotConfig coreCotConfig;
        /**
         * OKVS类型
         */
        private OkvsType okvsType;
        /**
         * 流水线数量
         */
        private int pipeSize;

        public Builder() {
            rpmtOprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            peqtOprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            okvsType = OkvsType.POLYNOMIAL;
            pipeSize = (1 << 8);
        }

        public Builder setRpmtOprfConfig(OprfConfig rpmtOprfConfig) {
            this.rpmtOprfConfig = rpmtOprfConfig;
            return this;
        }

        public Builder setPeqtOprfConfig(OprfConfig peqtOprfConfig) {
            this.peqtOprfConfig = peqtOprfConfig;
            return this;
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setOkvsType(OkvsType okvsType) {
            this.okvsType = okvsType;
            return this;
        }

        public Builder setPipeSize(int pipeSize) {
            MathPreconditions.checkPositive("pipeSize", pipeSize);
            this.pipeSize = pipeSize;
            return this;
        }

        @Override
        public Krtw19OriPsuConfig build() {
            return new Krtw19OriPsuConfig(this);
        }
    }
}
