package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk.hzf22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk.PkFkJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk.PkFkJoinFactory.PkFkJoinPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPermutationConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPermutationFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.SortSignConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.hzf22.Hzf22SortSignConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.hzf22.Hzf22PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalFactory;

/**
 * configure of HZF22 PKFK join.
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public class Hzf22PkFkJoinConfig extends AbstractMultiPartyPtoConfig implements PkFkJoinConfig {
    /**
     * config of pg sorter
     */
    private final PgSortConfig pgSortConfig;
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
    private final FillPermutationConfig fillPermutationConfig;
    /**
     * config of permutation fill protocol
     */
    private final SortSignConfig sortSignConfig;
    private Hzf22PkFkJoinConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        pgSortConfig = builder.pgSortConfig;
        permuteConfig = builder.permuteConfig;
        traversalConfig = builder.traversalConfig;
        fillPermutationConfig = builder.fillPermutationConfig;
        sortSignConfig = builder.sortSignConfig;
    }

    public PgSortConfig getPgSortConfig() {
        return pgSortConfig;
    }

    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    public TraversalConfig getTraversalConfig() {
        return traversalConfig;
    }

    public FillPermutationConfig getFillPermutationConfig() {
        return fillPermutationConfig;
    }

    public SortSignConfig getSortSignConfig() {
        return sortSignConfig;
    }

    @Override
    public PkFkJoinPtoType getPkFkJoinPtoType() {
        return PkFkJoinPtoType.PK_FK_JOIN_HZF22;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hzf22PkFkJoinConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * config of pg sorter
         */
        private PgSortConfig pgSortConfig;
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
        private final FillPermutationConfig fillPermutationConfig;
        /**
         * config of permutation fill protocol
         */
        private SortSignConfig sortSignConfig;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            SecurityModel securityModel = malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST;
            pgSortConfig = new Hzf22PgSortConfig.Builder(malicious).build();
            permuteConfig = PermuteFactory.createDefaultConfig(securityModel);
            traversalConfig = TraversalFactory.createDefaultConfig(securityModel);
            fillPermutationConfig = FillPermutationFactory.createDefaultConfig(securityModel);
            sortSignConfig = new Hzf22SortSignConfig.Builder(malicious).setPgSortConfig(pgSortConfig).build();
        }

        public Builder setPgSortConfig(PgSortConfig pgSortConfig){
            this.pgSortConfig = pgSortConfig;
            sortSignConfig = new Hzf22SortSignConfig.Builder(malicious).setPgSortConfig(pgSortConfig).build();
            return this;
        }

        @Override
        public Hzf22PkFkJoinConfig build() {
            return new Hzf22PkFkJoinConfig(this);
        }
    }
}
