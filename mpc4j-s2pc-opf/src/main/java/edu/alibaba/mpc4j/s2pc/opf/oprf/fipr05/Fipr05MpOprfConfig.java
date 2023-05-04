package edu.alibaba.mpc4j.s2pc.opf.oprf.fipr05;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;

/**
 * FIPR05 multi-query OPRF config.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public class Fipr05MpOprfConfig implements MpOprfConfig {
    /**
     * single-query OPRF config
     */
    private final SqOprfConfig sqOprfConfig;

    private Fipr05MpOprfConfig(Builder builder) {
        sqOprfConfig = builder.sqOprfConfig;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    @Override
    public OprfFactory.OprfType getPtoType() {
        return OprfFactory.OprfType.FIPR05;
    }

    @Override
    public void setEnvType(EnvType envType) {
        sqOprfConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return sqOprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (sqOprfConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = sqOprfConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Fipr05MpOprfConfig> {
        /**
         * single-query OPRF config
         */
        private SqOprfConfig sqOprfConfig;

        public Builder() {
            sqOprfConfig = SqOprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setSqOprfConfig(SqOprfConfig sqOprfConfig) {
            this.sqOprfConfig = sqOprfConfig;
            return this;
        }

        @Override
        public Fipr05MpOprfConfig build() {
            return new Fipr05MpOprfConfig(this);
        }
    }
}
