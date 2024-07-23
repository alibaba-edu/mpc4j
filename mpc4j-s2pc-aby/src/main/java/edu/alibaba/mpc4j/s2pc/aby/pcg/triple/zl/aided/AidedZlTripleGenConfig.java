package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.aided;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.ZlTripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.ZlTripleGenFactory.ZlTripleGenType;

/**
 * Aided Zl triple generation config.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
public class AidedZlTripleGenConfig extends AbstractMultiPartyPtoConfig implements ZlTripleGenConfig {

    private AidedZlTripleGenConfig() {
        super(SecurityModel.TRUSTED_DEALER);
    }

    @Override
    public ZlTripleGenType getPtoType() {
        return ZlTripleGenType.AIDED;
    }

    @Override
    public int defaultRoundNum(int l) {
        return Integer.MAX_VALUE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<AidedZlTripleGenConfig> {

        public Builder() {
            // empty
        }

        @Override
        public AidedZlTripleGenConfig build() {
            return new AidedZlTripleGenConfig();
        }
    }
}
