package edu.alibaba.mpc4j.s2pc.pjc.pid.bkms20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory.PidType;

/**
 * Facebook的椭圆曲线PID协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/19
 */
public class Bkms20EccPidConfig extends AbstractMultiPartyPtoConfig implements PidConfig {
    /**
     * 是否使用压缩椭圆曲线编码
     */
    private final boolean compressEncode;

    private Bkms20EccPidConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        compressEncode = builder.compressEncode;
    }

    @Override
    public PidType getPtoType() {
        return PidType.BKMS20_ECC;
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
