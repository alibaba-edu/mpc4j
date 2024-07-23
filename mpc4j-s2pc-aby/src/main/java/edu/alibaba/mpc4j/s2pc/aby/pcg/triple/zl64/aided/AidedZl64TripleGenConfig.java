package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.aided;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.Zl64TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.Zl64TripleGenFactory.Zl64TripleGenType;

/**
 * Aided Zl64 triple generation config.
 *
 * @author Weiran Liu
 * @date 2024/7/1
 */
public class AidedZl64TripleGenConfig extends AbstractMultiPartyPtoConfig implements Zl64TripleGenConfig {

    private AidedZl64TripleGenConfig() {
        super(SecurityModel.TRUSTED_DEALER);
    }

    @Override
    public Zl64TripleGenType getPtoType() {
        return Zl64TripleGenType.AIDED;
    }

    @Override
    public int defaultRoundNum(int l) {
        return Integer.MAX_VALUE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<AidedZl64TripleGenConfig> {

        public Builder() {
            // empty
        }

        @Override
        public AidedZl64TripleGenConfig build() {
            return new AidedZl64TripleGenConfig();
        }
    }
}
