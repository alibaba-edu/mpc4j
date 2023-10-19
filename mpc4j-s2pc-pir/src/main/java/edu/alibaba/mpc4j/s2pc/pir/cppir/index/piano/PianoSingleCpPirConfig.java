package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirFactory.SingleCpPirType;

/**
 * PIANO client-specific preprocessing PIR config.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class PianoSingleCpPirConfig extends AbstractMultiPartyPtoConfig implements SingleCpPirConfig {

    public PianoSingleCpPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleCpPirType getPtoType() {
        return SingleCpPirType.PIANO;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<PianoSingleCpPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public PianoSingleCpPirConfig build() {
            return new PianoSingleCpPirConfig(this);
        }
    }
}
