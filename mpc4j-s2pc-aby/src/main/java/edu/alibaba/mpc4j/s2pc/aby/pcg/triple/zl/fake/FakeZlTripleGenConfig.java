package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.fake;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.ZlTripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.ZlTripleGenFactory.ZlTripleGenType;

/**
 * fake Zl triple generation config.
 *
 * @author Weiran Liu
 * @date 2024/6/29
 */
public class FakeZlTripleGenConfig extends AbstractMultiPartyPtoConfig implements ZlTripleGenConfig {

    private FakeZlTripleGenConfig() {
        super(SecurityModel.IDEAL);
    }

    @Override
    public ZlTripleGenType getPtoType() {
        return ZlTripleGenType.FAKE;
    }

    @Override
    public int defaultRoundNum(int l) {
        return Integer.MAX_VALUE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<FakeZlTripleGenConfig> {

        public Builder() {
            // empty
        }

        @Override
        public FakeZlTripleGenConfig build() {
            return new FakeZlTripleGenConfig();
        }
    }
}
