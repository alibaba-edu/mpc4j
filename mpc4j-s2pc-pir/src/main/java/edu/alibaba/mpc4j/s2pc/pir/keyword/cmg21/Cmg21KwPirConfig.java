package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirFactory;

/**
 * CMG21关键词索引PIR协议配置项。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirConfig implements KwPirConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * 是否使用压缩椭圆曲线编码
     */
    private final boolean compressEncode;


    public Cmg21KwPirConfig(Builder builder) {
        this.envType = builder.envType;
        compressEncode = builder.compressEncode;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    @Override
    public KwPirFactory.KwPirType getProType() {
        return KwPirFactory.KwPirType.CMG21;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cmg21KwPirConfig> {
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
        public Cmg21KwPirConfig build() {
            return new Cmg21KwPirConfig(this);
        }
    }
}
