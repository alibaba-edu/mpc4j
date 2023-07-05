package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirFactory;

/**
 * CMG21 keyword PIR config.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirConfig extends AbstractMultiPartyPtoConfig implements KwPirConfig {
    /**
     * ecc point compress encode
     */
    private final boolean compressEncode;

    public Cmg21KwPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        compressEncode = builder.compressEncode;
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
         * ecc point compress encode
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
        public Cmg21KwPirConfig build() {
            return new Cmg21KwPirConfig(this);
        }
    }
}
