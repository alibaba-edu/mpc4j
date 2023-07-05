package edu.alibaba.mpc4j.s2pc.pcg.ot.base.co15;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;

/**
 * CO15-基础OT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/13
 */
public class Co15BaseOtConfig extends AbstractMultiPartyPtoConfig implements BaseOtConfig {
    /**
     * 是否使用压缩椭圆曲线编码
     */
    private final boolean compressEncode;

    private Co15BaseOtConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        compressEncode = builder.compressEncode;
    }

    @Override
    public BaseOtFactory.BaseOtType getPtoType() {
        return BaseOtFactory.BaseOtType.CO15;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Co15BaseOtConfig> {
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
        public Co15BaseOtConfig build() {
            return new Co15BaseOtConfig(this);
        }
    }
}
