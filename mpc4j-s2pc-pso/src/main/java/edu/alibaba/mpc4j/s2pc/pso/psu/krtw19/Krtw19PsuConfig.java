package edu.alibaba.mpc4j.s2pc.pso.psu.krtw19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;

/**
 * KRTW19-PSU config.
 *
 * @author Weiran Liu
 * @date 2022/02/20
 */
public class Krtw19PsuConfig extends AbstractMultiPartyPtoConfig implements PsuConfig {
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
     * 流水线数量
     */
    private final int pipeSize;

    private Krtw19PsuConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.rpmtOprfConfig, builder.peqtOprfConfig, builder.coreCotConfig);
        rpmtOprfConfig = builder.rpmtOprfConfig;
        peqtOprfConfig = builder.peqtOprfConfig;
        coreCotConfig = builder.coreCotConfig;
        pipeSize = builder.pipeSize;
    }

    @Override
    public PsuFactory.PsuType getPtoType() {
        return PsuFactory.PsuType.KRTW19;
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

    public int getPipeSize() {
        return pipeSize;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Krtw19PsuConfig> {
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
         * 流水线数量
         */
        private int pipeSize;

        public Builder() {
            rpmtOprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            peqtOprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
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

        public Builder setPipeSize(int pipeSize) {
            MathPreconditions.checkPositive("pipeSize", pipeSize);
            this.pipeSize = pipeSize;
            return this;
        }

        @Override
        public Krtw19PsuConfig build() {
            return new Krtw19PsuConfig(this);
        }
    }
}
