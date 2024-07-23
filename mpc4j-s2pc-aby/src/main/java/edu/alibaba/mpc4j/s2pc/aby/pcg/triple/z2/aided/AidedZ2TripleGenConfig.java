package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.aided;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenFactory.Z2TripleGenType;

/**
 * Aided Z2 triple generation config.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
public class AidedZ2TripleGenConfig extends AbstractMultiPartyPtoConfig implements Z2TripleGenConfig {

    private AidedZ2TripleGenConfig() {
        super(SecurityModel.TRUSTED_DEALER);
    }

    @Override
    public Z2TripleGenType getPtoType() {
        return Z2TripleGenType.AIDED;
    }

    @Override
    public int defaultRoundNum() {
        return Integer.MAX_VALUE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<AidedZ2TripleGenConfig> {

        public Builder() {
            // empty
        }

        @Override
        public AidedZ2TripleGenConfig build() {
            return new AidedZ2TripleGenConfig();
        }
    }
}
