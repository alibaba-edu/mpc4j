package edu.alibaba.mpc4j.s2pc.pso.pid.bkms20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidFactory.PidType;

/**
 * Facebook的椭圆曲线PID协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/19
 */
public class Bkms20EccPidConfig implements PidConfig {
    /**
     * 环境类型
     */
    private EnvType envType;
    /**
     * 是否使用压缩椭圆曲线编码
     */
    private final boolean compressEncode;

    private Bkms20EccPidConfig(Builder builder) {
        compressEncode = builder.compressEncode;
        envType = EnvType.STANDARD;
    }

    @Override
    public PidType getPtoType() {
        return PidType.BKMS20_ECC;
    }

    @Override
    public void setEnvType(EnvType envType) {
        this.envType = envType;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bkms20EccPidConfig> {
        /**
         * 是否使用压缩椭圆曲线编码
         */
        private boolean compressEncode;

        public Builder() {
            compressEncode = true;
        }

        public Builder setCompressEncode(boolean compressEncode) {
            this.compressEncode = compressEncode;
            return this;
        }

        @Override
        public Bkms20EccPidConfig build() {
            return new Bkms20EccPidConfig(this);
        }
    }
}
