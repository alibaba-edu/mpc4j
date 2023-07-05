package edu.alibaba.mpc4j.s2pc.pcg.ot.base.csw20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;

/**
 * Csw20-基础OT协议配置项。
 *
 * @author Hanwen Feng
 * @date 2022/04/26
 */
public class Csw20BaseOtConfig extends AbstractMultiPartyPtoConfig implements BaseOtConfig {
    /**
     * 是否使用压缩椭圆曲线编码
     */
    private final boolean compressEncode;

    private Csw20BaseOtConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        compressEncode = builder.compressEncode;
    }

    @Override
    public BaseOtFactory.BaseOtType getPtoType() {
        return BaseOtFactory.BaseOtType.CSW20;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Csw20BaseOtConfig> {
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
        public Csw20BaseOtConfig build() {
            return new Csw20BaseOtConfig(this);
        }
    }
}
