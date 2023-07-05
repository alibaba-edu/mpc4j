package edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;

/**
 * NP01-字节基础OT协议配置项。
 *
 * @author Weiran Liu
 * @date 2023/4/24
 */
public class Np01ByteBaseOtConfig extends AbstractMultiPartyPtoConfig implements BaseOtConfig {

    private Np01ByteBaseOtConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public BaseOtFactory.BaseOtType getPtoType() {
        return BaseOtFactory.BaseOtType.NP01_BYTE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Np01ByteBaseOtConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Np01ByteBaseOtConfig build() {
            return new Np01ByteBaseOtConfig(this);
        }
    }
}
