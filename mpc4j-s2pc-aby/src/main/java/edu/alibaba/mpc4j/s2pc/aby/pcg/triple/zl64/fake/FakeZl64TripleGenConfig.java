package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.fake;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.Zl64TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.Zl64TripleGenFactory.Zl64TripleGenType;

/**
 * fake Zl64 triple generation config.
 *
 * @author Weiran Liu
 * @date 2024/6/30
 */
public class FakeZl64TripleGenConfig extends AbstractMultiPartyPtoConfig implements Zl64TripleGenConfig {

    private FakeZl64TripleGenConfig() {
        super(SecurityModel.IDEAL);
    }

    @Override
    public Zl64TripleGenType getPtoType() {
        return Zl64TripleGenType.FAKE;
    }

    @Override
    public int defaultRoundNum(int l) {
        return Integer.MAX_VALUE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<FakeZl64TripleGenConfig> {

        public Builder() {
            // empty
        }

        @Override
        public FakeZl64TripleGenConfig build() {
            return new FakeZl64TripleGenConfig();
        }
    }
}
