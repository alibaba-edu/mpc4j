package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirFactory.SingleIndexCpPirType;

/**
 * PIANO client-specific preprocessing PIR config.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class PianoSingleIndexCpPirConfig extends AbstractMultiPartyPtoConfig implements SingleIndexCpPirConfig {

    public PianoSingleIndexCpPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleIndexCpPirType getProType() {
        return SingleIndexCpPirType.ZPSZ23_PIANO;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<PianoSingleIndexCpPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public PianoSingleIndexCpPirConfig build() {
            return new PianoSingleIndexCpPirConfig(this);
        }
    }
}
