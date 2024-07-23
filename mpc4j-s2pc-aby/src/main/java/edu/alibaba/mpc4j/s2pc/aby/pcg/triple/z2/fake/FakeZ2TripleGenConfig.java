package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.fake;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenFactory.Z2TripleGenType;

/**
 * fake Z2 triple generation config.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
public class FakeZ2TripleGenConfig extends AbstractMultiPartyPtoConfig implements Z2TripleGenConfig {

    private FakeZ2TripleGenConfig() {
        super(SecurityModel.IDEAL);
    }

    @Override
    public Z2TripleGenType getPtoType() {
        return Z2TripleGenType.FAKE;
    }

    @Override
    public int defaultRoundNum() {
        return Integer.MAX_VALUE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<FakeZ2TripleGenConfig> {

        public Builder() {
            // empty
        }

        @Override
        public FakeZ2TripleGenConfig build() {
            return new FakeZ2TripleGenConfig();
        }
    }
}
