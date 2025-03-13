package edu.alibaba.work.femur.demo.seal;


import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.work.femur.FemurSealPirParams;
import edu.alibaba.work.femur.demo.FemurDemoPirConfig;
import edu.alibaba.work.femur.demo.FemurDemoPirType;

/**
 * SEAL Femur demo PIR config.
 *
 * @author Liqiang Peng
 * @date 2024/9/19
 */
public class SealFemurDemoMemoryPirConfig implements FemurDemoPirConfig {
    /**
     * SEAL PIR params
     */
    private final FemurSealPirParams params;
    /**
     * whether to use differential privacy
     */
    private final boolean dp;
    /**
     * epsilon range used to build this index
     */
    private final int pgmIndexLeafEpsilon;

    public SealFemurDemoMemoryPirConfig(Builder builder) {
        this.params = builder.params;
        this.dp = builder.dp;
        this.pgmIndexLeafEpsilon = builder.pgmIndexLeafEpsilon;
    }

    @Override
    public FemurDemoPirType getPtoType() {
        return FemurDemoPirType.SEAL_MEMORY;
    }

    /**
     * Returns SEAL PIR params.
     *
     * @return SEAL PIR params.
     */
    public FemurSealPirParams getParams() {
        return params;
    }

    /**
     * Returns whether to use differential privacy when querying the range.
     *
     * @return true if using differential privacy; false otherwise.
     */
    public boolean isDp() {
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SealFemurDemoMemoryPirConfig> {
        /**
         * SEAL PIR params
         */
        private FemurSealPirParams params;
        /**
         * whether to use differential privacy
         */
        private boolean dp;
        /**
         * epsilon range used to build this index
         */
        private int pgmIndexLeafEpsilon;

        public Builder() {
            params = FemurSealPirParams.DEFAULT_PARAMS;
            dp = false;
            pgmIndexLeafEpsilon = CommonConstants.PGM_INDEX_LEAF_EPSILON;
        }

        public Builder setParams(FemurSealPirParams params) {
            this.params = params;
            return this;
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
        public SealFemurDemoMemoryPirConfig build() {
            return new SealFemurDemoMemoryPirConfig(this);
        }
    }
}
