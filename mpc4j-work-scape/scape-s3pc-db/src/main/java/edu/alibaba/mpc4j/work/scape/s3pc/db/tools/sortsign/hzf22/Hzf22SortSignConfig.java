package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.hzf22;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.SortSignConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.SortSignFactory.SortSignType;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.MergeConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.MergeFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.hzf22.Hzf22PgSortConfig;

/**
 * HZF22 SortSign Config
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public class Hzf22SortSignConfig extends AbstractMultiPartyPtoConfig implements SortSignConfig {
    /**
     * config of pg sorter
     */
    private final PgSortConfig pgSortConfig;
    /**
     * config of merge sorter
     */
    private final MergeConfig mergeConfig;
    /**
     * config of permutation
     */
    private final PermuteConfig permuteConfig;

    private Hzf22SortSignConfig(Builder builder) {
        super(builder.pgSortConfig.getSecurityModel(), builder.pgSortConfig);
        pgSortConfig = builder.pgSortConfig;
        mergeConfig = builder.mergeConfig;
        permuteConfig = builder.permuteConfig;
    }

    @Override
    public SortSignType getSortSignType() {
        return SortSignType.HZF22;
    }

    public PgSortConfig getPgSortConfig() {
        return pgSortConfig;
    }

    public MergeConfig getMergeConfig() {
        return mergeConfig;
    }

    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hzf22SortSignConfig> {
        /**
         * config of pg sorter
         */
        private PgSortConfig pgSortConfig;
        /**
         * config of pg sorter
         */
        private final MergeConfig mergeConfig;
        /**
         * config of permutation
         */
        private final PermuteConfig permuteConfig;

        public Builder(boolean malicious) {
            pgSortConfig = new Hzf22PgSortConfig.Builder(malicious).build();
            mergeConfig = MergeFactory.createDefaultConfig(pgSortConfig.getSecurityModel());
            permuteConfig = PermuteFactory.createDefaultConfig(pgSortConfig.getSecurityModel());
        }

        public Builder setPgSortConfig(PgSortConfig pgSortConfig) {
            this.pgSortConfig = pgSortConfig;
            return this;
        }

        @Override
        public Hzf22SortSignConfig build() {
            return new Hzf22SortSignConfig(this);
        }
    }
}
