package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory.MspCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotConfig;

/**
 * YWL20-UNI-MSP-COT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class Ywl20UniMspCotConfig implements MspCotConfig {
    /**
     * BSPCOT协议配置项
     */
    private final BspCotConfig bspCotConfig;

    private Ywl20UniMspCotConfig(Builder builder) {
        bspCotConfig = builder.bspcotConfig;
    }

    public BspCotConfig getBspCotConfig() {
        return bspCotConfig;
    }

    @Override
    public MspCotType getPtoType() {
        return MspCotType.YWL20_UNI;
    }

    @Override
    public void setEnvType(EnvType envType) {
        bspCotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return bspCotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.MALICIOUS;
        if (bspCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = bspCotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20UniMspCotConfig> {
        /**
         * BSPCOT协议配置项
         */
        private BspCotConfig bspcotConfig;

        public Builder(SecurityModel securityModel) {
            this.bspcotConfig = BspCotFactory.createDefaultConfig(securityModel);
        }

        public Builder setBspcotConfig(BspCotConfig bspcotConfig) {
            this.bspcotConfig = bspcotConfig;
            return this;
        }

        @Override
        public Ywl20UniMspCotConfig build() {
            return new Ywl20UniMspCotConfig(this);
        }
    }
}
