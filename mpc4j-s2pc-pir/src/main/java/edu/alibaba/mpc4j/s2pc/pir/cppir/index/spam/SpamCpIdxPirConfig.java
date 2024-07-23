package edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;

/**
 * SPAM client-specific preprocessing index PIR config.
 *
 * @author Weiran Liu
 * @date 2023/8/31
 */
public class SpamCpIdxPirConfig extends AbstractMultiPartyPtoConfig implements CpIdxPirConfig {

    public SpamCpIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public CpIdxPirType getPtoType() {
        return CpIdxPirType.SPAM;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SpamCpIdxPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public SpamCpIdxPirConfig build() {
            return new SpamCpIdxPirConfig(this);
        }
    }
}
