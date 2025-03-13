package edu.alibaba.work.femur.seal;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.work.femur.FemurRpcPirConfig;
import edu.alibaba.work.femur.FemurRpcPirFactory;
import edu.alibaba.work.femur.FemurSealPirParams;

/**
 * PGM-index range SEAL PIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/9/10
 */
public class SealFemurRpcPirConfig extends AbstractMultiPartyPtoConfig implements FemurRpcPirConfig {
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

    public SealFemurRpcPirConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        this.params = builder.params;
        this.dp = builder.dp;
        this.pgmIndexLeafEpsilon = builder.pgmIndexLeafEpsilon;
    }

    @Override
    public FemurRpcPirFactory.FemurPirType getPtoType() {
        return FemurRpcPirFactory.FemurPirType.PGM_INDEX_SEAL_PIR;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SealFemurRpcPirConfig> {
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
        public SealFemurRpcPirConfig build() {
            return new SealFemurRpcPirConfig(this);
        }
    }
}
