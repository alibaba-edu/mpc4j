package edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirFactory.SingleIndexCpPirType;

/**
 * SPAM client-specific preprocessing PIR config.
 *
 * @author Weiran Liu
 * @date 2023/8/31
 */
public class SpamSingleIndexCpPirConfig extends AbstractMultiPartyPtoConfig implements SingleIndexCpPirConfig {

    public SpamSingleIndexCpPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleIndexCpPirType getProType() {
        return SingleIndexCpPirType.MIR23_SPAM;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SpamSingleIndexCpPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public SpamSingleIndexCpPirConfig build() {
            return new SpamSingleIndexCpPirConfig(this);
        }
    }
}
