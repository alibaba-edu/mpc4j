package edu.alibaba.mpc4j.work.db.sketch.utils.truncate.ext;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.TruncateConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.TruncateFactory.TruncatePtoType;

/**
 * Configuration for the extended (EXT) implementation of Truncate protocol.
 * Uses oblivious permutation for efficient truncation of secret-shared data.
 */
public class ExtTruncateConfig extends AbstractMultiPartyPtoConfig implements TruncateConfig {
    /**
     * Configuration for oblivious permutation operations
     */
    private final PermuteConfig permuteConfig;

    private ExtTruncateConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        permuteConfig = builder.permuteConfig;
    }

    @Override
    public TruncatePtoType getPtoType() {
        return TruncatePtoType.EXT;
    }

    /**
     * Get the oblivious permutation configuration
     *
     * @return the permutation configuration
     */
    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    /**
     * Builder for creating ExtTruncateConfig instances
     */
    public static class Builder implements org.apache.commons.lang3.builder.Builder<ExtTruncateConfig> {
        /**
         * Whether to use malicious security model
         */
        private final boolean malicious;
        /**
         * Configuration for oblivious permutation operations
         */
        private final PermuteConfig permuteConfig;

        /**
         * Constructor for Builder
         *
         * @param malicious whether to use malicious security model
         */
        public Builder(boolean malicious) {
            this.malicious = malicious;
            permuteConfig = PermuteFactory.createDefaultConfig(malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        }

        @Override
        public ExtTruncateConfig build() {
            return new ExtTruncateConfig(this);
        }
    }
}
