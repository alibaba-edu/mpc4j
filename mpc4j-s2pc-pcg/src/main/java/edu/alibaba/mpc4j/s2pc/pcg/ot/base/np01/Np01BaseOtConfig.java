package edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory.BaseOtType;

/**
 * NP01-基础OT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/13
 */
public class Np01BaseOtConfig implements BaseOtConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * 是否使用压缩椭圆曲线编码
     */
    private final boolean compressEncode;

    private Np01BaseOtConfig(Builder builder) {
        envType = builder.envType;
        compressEncode = builder.compressEncode;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.MALICIOUS;
    }

    @Override
    public BaseOtType getPtoType() {
        return BaseOtType.NP01;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Np01BaseOtConfig> {
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
        public Np01BaseOtConfig build() {
            return new Np01BaseOtConfig(this);
        }
    }
}
