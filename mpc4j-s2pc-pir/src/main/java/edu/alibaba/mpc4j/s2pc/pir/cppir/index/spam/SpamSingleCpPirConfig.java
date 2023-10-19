package edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirFactory.SingleCpPirType;

/**
 * SPAM client-specific preprocessing PIR config.
 *
 * @author Weiran Liu
 * @date 2023/8/31
 */
public class SpamSingleCpPirConfig extends AbstractMultiPartyPtoConfig implements SingleCpPirConfig {

    public SpamSingleCpPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleCpPirType getPtoType() {
        return SingleCpPirType.SPAM;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SpamSingleCpPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public SpamSingleCpPirConfig build() {
            return new SpamSingleCpPirConfig(this);
        }
    }
}
