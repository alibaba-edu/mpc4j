package edu.alibaba.mpc4j.work.db.sketch.utils.truncate.ext;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.TruncateConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.TruncateFactory.TruncatePtoType;

public class ExtTruncateConfig extends AbstractMultiPartyPtoConfig implements TruncateConfig {
    /**
     * config of oblivious traversal
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

    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<ExtTruncateConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * config of oblivious traversal
         */
        private final PermuteConfig permuteConfig;

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
