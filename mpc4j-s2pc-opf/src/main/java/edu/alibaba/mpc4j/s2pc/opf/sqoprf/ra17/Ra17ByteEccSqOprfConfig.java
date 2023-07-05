package edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;

/**
 * RA17 byte ECC single-query OPRF config.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public class Ra17ByteEccSqOprfConfig extends AbstractMultiPartyPtoConfig implements SqOprfConfig {

    private Ra17ByteEccSqOprfConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SqOprfFactory.SqOprfType getPtoType() {
        return SqOprfFactory.SqOprfType.RA17_BYTE_ECC;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ra17ByteEccSqOprfConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Ra17ByteEccSqOprfConfig build() {
            return new Ra17ByteEccSqOprfConfig(this);
        }
    }
}
