package edu.alibaba.mpc4j.work.db.sketch.CMS.v2;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22ext.Hzf22ExtGroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpFactory;
import edu.alibaba.mpc4j.work.db.sketch.CMS.CMSConfig;
import edu.alibaba.mpc4j.work.db.sketch.CMS.CMSFactory;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.TruncateConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.TruncateFactory;

/**
 * v2 CMS protocol
 */
public class v2CMSConfig extends AbstractMultiPartyPtoConfig implements CMSConfig {
    /**
     * config of oblivious permutation
     */
    private final PermuteConfig permuteConfig;
    /**
     * config of group sum
     */
    private final GroupSumConfig groupSumConfig;
    /**
     * config of pg sorter
     */
    private final PgSortConfig pgSortConfig;
    /**
     * config of soprp
     */
    private final SoprpConfig soprpConfig;
    /**
     * config of truncate
     */
    private final TruncateConfig truncateConfig;

    private v2CMSConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        permuteConfig = builder.permuteConfig;
        groupSumConfig = builder.groupSumConfig;
        pgSortConfig = builder.pgSortConfig;
        soprpConfig = builder.soprpConfig;
        truncateConfig = builder.truncateConfig;
    }

    @Override
    public CMSFactory.CMSPtoType getPtoType() {
        return CMSFactory.CMSPtoType.CMS_V2;
    }

    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    public GroupSumConfig getGroupSumConfig() {
        return groupSumConfig;
    }

    public PgSortConfig getPgSortConfig() {
        return pgSortConfig;
    }

    public SoprpConfig getSoprpConfig() {
        return soprpConfig;
    }
    public TruncateConfig getTruncateConfig() {
        return truncateConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<v2CMSConfig> {
        /**
         * malicious or not
         */
        private final boolean malicious;
        /**
         * config of oblivious permutation
         */
        private final PermuteConfig permuteConfig;
        /**
         * config of group sum
         */
        private final GroupSumConfig groupSumConfig;

        private final SoprpConfig soprpConfig;
        /**
         * config of pg sorter
         */
        private PgSortConfig pgSortConfig;
        /**
         * config of truncate
         */
        private final TruncateConfig truncateConfig;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            permuteConfig = PermuteFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            groupSumConfig = new Hzf22ExtGroupSumConfig.Builder(malicious).build();
            pgSortConfig = PgSortFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            soprpConfig = SoprpFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, 64);
            truncateConfig = TruncateFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setPgSortConfig(PgSortConfig pgSortConfig) {
            this.pgSortConfig = pgSortConfig;
            return this;
        }

        @Override
        public v2CMSConfig build() {
            return new v2CMSConfig(this);
        }
    }
}
