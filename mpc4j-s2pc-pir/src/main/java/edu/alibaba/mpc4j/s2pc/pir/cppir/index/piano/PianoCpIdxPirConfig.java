package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;

/**
 * PIANO client-specific preprocessing index PIR config.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class PianoCpIdxPirConfig extends AbstractMultiPartyPtoConfig implements CpIdxPirConfig {

    public PianoCpIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public CpIdxPirType getPtoType() {
        return CpIdxPirType.PIANO;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<PianoCpIdxPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public PianoCpIdxPirConfig build() {
            return new PianoCpIdxPirConfig(this);
        }
    }
}
