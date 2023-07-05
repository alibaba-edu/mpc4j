package edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;

/**
 * RA17 ECC single-query OPRF config.
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class Ra17EccSqOprfConfig extends AbstractMultiPartyPtoConfig implements SqOprfConfig {
    /**
     * compress encode
     */
    private final boolean compressEncode;

    private Ra17EccSqOprfConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        compressEncode = builder.compressEncode;
    }

    @Override
    public SqOprfFactory.SqOprfType getPtoType() {
        return SqOprfFactory.SqOprfType.RA17_ECC;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ra17EccSqOprfConfig> {
        /**
         * compress encode
         */
        private boolean compressEncode;

        public Builder() {
            compressEncode = true;
        }

        public Ra17EccSqOprfConfig.Builder setCompressEncode(boolean compressEncode) {
            this.compressEncode = compressEncode;
            return this;
        }

        @Override
        public Ra17EccSqOprfConfig build() {
            return new Ra17EccSqOprfConfig(this);
        }
    }
}
