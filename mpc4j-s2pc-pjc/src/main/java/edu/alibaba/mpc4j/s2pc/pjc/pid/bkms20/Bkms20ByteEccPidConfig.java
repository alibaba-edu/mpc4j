package edu.alibaba.mpc4j.s2pc.pjc.pid.bkms20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;

/**
 * Facebook的字节椭圆曲线PID协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/9/13
 */
public class Bkms20ByteEccPidConfig extends AbstractMultiPartyPtoConfig implements PidConfig {

    private Bkms20ByteEccPidConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
    }

    @Override
    public PidFactory.PidType getPtoType() {
        return PidFactory.PidType.BKMS20_BYTE_ECC;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bkms20ByteEccPidConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Bkms20ByteEccPidConfig build() {
            return new Bkms20ByteEccPidConfig(this);
        }
    }
}
