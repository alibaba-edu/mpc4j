package edu.alibaba.mpc4j.s2pc.pso.oprp.lowmc;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprp.OprpConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprp.OprpFactory.OprpType;

/**
 * LowMc-OPRP协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public class LowMcOprpConfig implements OprpConfig {
    /**
     * BC协议
     */
    private final BcConfig bcConfig;

    private LowMcOprpConfig(Builder builder) {
        bcConfig = builder.bcConfig;
    }

    public BcConfig getBcConfig() {
        return bcConfig;
    }

    @Override
    public OprpType getPtoType() {
        return OprpType.LOW_MC;
    }

    @Override
    public void setEnvType(EnvType envType) {
        bcConfig.setEnvType(envType);
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
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<LowMcOprpConfig> {
        /**
         * BC协议配置项
         */
        private BcConfig bcConfig;

        public Builder() {
            bcConfig = BcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setBcConfig(BcConfig bcConfig) {
            this.bcConfig = bcConfig;
            return this;
        }

        @Override
        public LowMcOprpConfig build() {
            return new LowMcOprpConfig(this);
        }
    }
}
