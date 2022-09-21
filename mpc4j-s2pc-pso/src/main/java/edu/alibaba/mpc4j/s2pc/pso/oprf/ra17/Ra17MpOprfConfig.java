package edu.alibaba.mpc4j.s2pc.pso.oprf.ra17;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;

/**
 * RA17-MPOPRF协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/06
 */
public class Ra17MpOprfConfig implements MpOprfConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * 是否使用压缩椭圆曲线编码
     */
    private final boolean compressEncode;

    private Ra17MpOprfConfig(Builder builder) {
        envType = builder.envType;
        compressEncode = builder.compressEncode;
    }

    @Override
    public OprfFactory.OprfType getPtoType() {
        return OprfFactory.OprfType.RA17;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ra17MpOprfConfig> {
        /**
         * 环境类型
         */
        private EnvType envType;
        /**
         * 是否使用压缩椭圆曲线编码
         */
        private boolean compressEncode;

        public Builder() {
            envType = EnvType.STANDARD;
            compressEncode = true;
        }

        public Builder setEnvType(EnvType envType) {
            this.envType = envType;
            return this;
        }

        public Builder setCompressEncode(boolean compressEncode) {
            this.compressEncode = compressEncode;
            return this;
        }

        @Override
        public Ra17MpOprfConfig build() {
            return new Ra17MpOprfConfig(this);
        }
    }
}
