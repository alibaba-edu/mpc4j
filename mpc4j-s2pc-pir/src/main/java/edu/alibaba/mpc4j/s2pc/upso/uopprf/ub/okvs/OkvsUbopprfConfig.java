package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.okvs;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.UbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.UbopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;

/**
 * OKVS unbalanced batched OPPRF config.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public class OkvsUbopprfConfig implements UbopprfConfig {
    /**
     * single-query OPRF config
     */
    private final SqOprfConfig sqOprfConfig;
    /**
     * OKVS type
     */
    private final OkvsFactory.OkvsType okvsType;

    private OkvsUbopprfConfig(Builder builder) {
        sqOprfConfig = builder.sqOprfConfig;
        okvsType = builder.okvsType;
    }

    @Override
    public UbopprfFactory.UbopprfType getPtoType() {
        return UbopprfFactory.UbopprfType.OKVS;
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
        return SecurityModel.SEMI_HONEST;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    public OkvsFactory.OkvsType getOkvsType() {
        return okvsType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<OkvsUbopprfConfig> {
        /**
         * single-point OPRF config
         */
        private SqOprfConfig sqOprfConfig;
        /**
         * OKVS type
         */
        private OkvsFactory.OkvsType okvsType;

        public Builder() {
            sqOprfConfig = SqOprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            okvsType = OkvsFactory.OkvsType.H3_SINGLETON_GCT;
        }

        public Builder setSqOprfConfig(SqOprfConfig sqOprfConfig) {
            this.sqOprfConfig = sqOprfConfig;
            return this;
        }

        public Builder setOkvsType(OkvsFactory.OkvsType okvsType) {
            this.okvsType = okvsType;
            return this;
        }

        @Override
        public OkvsUbopprfConfig build() {
            return new OkvsUbopprfConfig(this);
        }
    }
}
