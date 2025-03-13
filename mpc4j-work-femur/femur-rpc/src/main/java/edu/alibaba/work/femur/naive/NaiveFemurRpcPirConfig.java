package edu.alibaba.work.femur.naive;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.work.femur.FemurRpcPirConfig;
import edu.alibaba.work.femur.FemurRpcPirFactory;

/**
 * PGM-index range naive PIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/9/19
 */
public class NaiveFemurRpcPirConfig extends AbstractMultiPartyPtoConfig implements FemurRpcPirConfig {
    /**
     * whether to use differential privacy
     */
    private final boolean dp;
    /**
     * epsilon range used to build this index
     */
    private final int pgmIndexLeafEpsilon;

    public NaiveFemurRpcPirConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        this.dp = builder.dp;
        pgmIndexLeafEpsilon = builder.pgmIndexLeafEpsilon;
    }

    @Override
    public FemurRpcPirFactory.FemurPirType getPtoType() {
        return FemurRpcPirFactory.FemurPirType.PGM_INDEX_NAIVE_PIR;
    }

    /**
     * Returns whether to use differential privacy when querying the range.
     *
     * @return true if using differential privacy; false otherwise.
     */
    public boolean getDp() {
        return dp;
    }

    /**
     * Returns epsilon range used to build this index.
     *
     * @return epsilon range used to build this index.
     */
    public int getPgmIndexLeafEpsilon() {
        return pgmIndexLeafEpsilon;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaiveFemurRpcPirConfig> {
        /**
         * whether to use differential privacy
         */
        private boolean dp;
        /**
         * epsilon range used to build this index
         */
        private int pgmIndexLeafEpsilon;

        public Builder() {
            dp = false;
            pgmIndexLeafEpsilon = CommonConstants.PGM_INDEX_LEAF_EPSILON;
        }

        public Builder setDp(boolean dp) {
            this.dp = dp;
            return this;
        }

        public Builder setPgmIndexLeafEpsilon(int pgmIndexLeafEpsilon) {
            this.pgmIndexLeafEpsilon = pgmIndexLeafEpsilon;
            return this;
        }

        @Override
        public NaiveFemurRpcPirConfig build() {
            return new NaiveFemurRpcPirConfig(this);
        }
    }
}
