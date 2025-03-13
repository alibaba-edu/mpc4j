package edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general.hzf22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general.GeneralSemiJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general.GeneralSemiJoinFactory.GeneralSemiJoinPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.SortSignConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.hzf22.Hzf22SortSignConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalFactory;

/**
 * HZF22 general semi-join config.
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public class Hzf22GeneralSemiJoinConfig extends AbstractMultiPartyPtoConfig implements GeneralSemiJoinConfig {
    /**
     * configure of permuation
     */
    private final PermuteConfig permuteConfig;
    /**
     * config of oblivious traversal
     */
    private final TraversalConfig traversalConfig;
    /**
     * config of permutation fill protocol
     */
    private final SortSignConfig sortSignConfig;

    private Hzf22GeneralSemiJoinConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        permuteConfig = builder.permuteConfig;
        traversalConfig = builder.traversalConfig;
        sortSignConfig = builder.sortSignConfig;
    }

    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    public TraversalConfig getTraversalConfig() {
        return traversalConfig;
    }

    public SortSignConfig getSortSignConfig() {
        return sortSignConfig;
    }

    @Override
    public GeneralSemiJoinPtoType getGeneralSemiJoinPtoType() {
        return GeneralSemiJoinPtoType.GENERAL_SEMI_JOIN_HZF22;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hzf22GeneralSemiJoinConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * configure of permuation
         */
        private final PermuteConfig permuteConfig;
        /**
         * config of oblivious traversal
         */
        private final TraversalConfig traversalConfig;
        /**
         * config of permutation fill protocol
         */
        private SortSignConfig sortSignConfig;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            SecurityModel securityModel = malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST;
            permuteConfig = PermuteFactory.createDefaultConfig(securityModel);
            traversalConfig = TraversalFactory.createDefaultConfig(securityModel);
            sortSignConfig = new Hzf22SortSignConfig.Builder(malicious).build();
        }

        public Builder setPgSortConfig(PgSortConfig pgSortConfig) {
            sortSignConfig = new Hzf22SortSignConfig.Builder(malicious).setPgSortConfig(pgSortConfig).build();
            return this;
        }

        @Override
        public Hzf22GeneralSemiJoinConfig build() {
            return new Hzf22GeneralSemiJoinConfig(this);
        }
    }
}
