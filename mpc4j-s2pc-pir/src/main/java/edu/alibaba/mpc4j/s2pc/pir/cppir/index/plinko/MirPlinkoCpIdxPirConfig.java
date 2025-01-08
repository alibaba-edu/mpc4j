package edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;

/**
 * MIR-based Plinko client-preprocessing index PIR config.
 *
 * @author Weiran Liu
 * @date 2024/10/11
 */
public class MirPlinkoCpIdxPirConfig extends AbstractMultiPartyPtoConfig implements CpIdxPirConfig {
    /**
     * number of queries for each round
     */
    private final int q;

    public MirPlinkoCpIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        q = builder.q;
    }

    /**
     * Sets number of queries in each round.
     *
     * @return number of queries in each round.
     */
    public int getQ() {
        return q;
    }

    @Override
    public CpIdxPirType getPtoType() {
        return CpIdxPirType.MIR_PLINKO;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<MirPlinkoCpIdxPirConfig> {
        /**
         * number of queries for each round
         */
        private int q;

        public Builder() {
            q = -1;
        }

        /**
         * Sets the number of queries for each round
         */
        public Builder setQ(int q) {
            MathPreconditions.checkPositive("q", q);
            this.q = q;
            return this;
        }

        @Override
        public MirPlinkoCpIdxPirConfig build() {
            return new MirPlinkoCpIdxPirConfig(this);
        }
    }
}
