package edu.alibaba.work.femur.demo.naive;


import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.work.femur.demo.FemurDemoPirConfig;
import edu.alibaba.work.femur.demo.FemurDemoPirType;

/**
 * Naive Femur demo memory PIR config.
 *
 * @author Liqiang Peng
 * @date 2024/9/19
 */
public class NaiveFemurDemoMemoryPirConfig implements FemurDemoPirConfig {
    /**
     * whether to use differential privacy
     */
    private final boolean dp;
    /**
     * epsilon range used to build this index
     */
    private final int pgmIndexLeafEpsilon;

    public NaiveFemurDemoMemoryPirConfig(Builder builder) {
        dp = builder.dp;
        pgmIndexLeafEpsilon = builder.pgmIndexLeafEpsilon;
    }

    @Override
    public FemurDemoPirType getPtoType() {
        return FemurDemoPirType.NAIVE_MEMORY;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaiveFemurDemoMemoryPirConfig> {
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
        public NaiveFemurDemoMemoryPirConfig build() {
            return new NaiveFemurDemoMemoryPirConfig(this);
        }
    }
}
