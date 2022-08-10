package edu.alibaba.mpc4j.s2pc.pso.pid.bkms20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidFactory.PidType;

/**
 * Facebook的PID协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/19
 */
public class Bkms20PidConfig implements PidConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * 是否使用压缩椭圆曲线编码
     */
    private final boolean compressEncode;

    private Bkms20PidConfig(Builder builder) {
        envType = builder.envType;
        compressEncode = builder.compressEncode;
    }

    @Override
    public PidType getPtoType() {
        return PidType.BKMS20;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bkms20PidConfig> {
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
            this.envType = EnvType.STANDARD;
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
        public Bkms20PidConfig build() {
            return new Bkms20PidConfig(this);
        }
    }
}
