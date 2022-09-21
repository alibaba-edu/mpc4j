package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BaseNotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BaseNotFactory;

/**
 * NP01-基础n选1-OT协议配置项。
 *
 * @author Hanwen Feng
 * @date 2022/07/26
 */
public class Np01BaseNotConfig implements BaseNotConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * 是否使用压缩椭圆曲线编码
     */
    private final boolean compressEncode;

    private Np01BaseNotConfig(Builder builder) {
        envType = builder.envType;
        compressEncode = builder.compressEncode;
    }

    @Override
    public BaseNotFactory.BaseNotType getPtoType() {
        return BaseNotFactory.BaseNotType.NP01;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.MALICIOUS;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Np01BaseNotConfig> {
        /**
         * 环境类型
         */
        private EnvType envType;
        /**
         * 是否使用压缩椭圆曲线编码
         */
        private boolean compressEncode;

        public Builder() {
            super();
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
        public Np01BaseNotConfig build() {
            return new Np01BaseNotConfig(this);
        }
    }
}
