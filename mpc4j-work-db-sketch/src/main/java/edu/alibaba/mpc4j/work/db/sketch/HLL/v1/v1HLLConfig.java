package edu.alibaba.mpc4j.work.db.sketch.HLL.v1;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalFactory;
import edu.alibaba.mpc4j.work.db.sketch.HLL.HLLConfig;
import edu.alibaba.mpc4j.work.db.sketch.HLL.HLLFactory.HLLPtoType;
import edu.alibaba.mpc4j.work.db.sketch.utils.agg.AggConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.agg.AggFactory;

/**
 * v1 HLL config.
 */
public class v1HLLConfig extends AbstractMultiPartyPtoConfig implements HLLConfig {
    private final PermuteConfig permuteConfig;

    private final AggConfig aggConfig;

    private final TraversalConfig traversalConfig;
    private final GroupExtremeConfig groupExtremeConfig;

    private final PgSortConfig pgSortConfig;

    private final SoprpConfig soprpConfig;

    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }
    public AggConfig getAggConfig() {return aggConfig;}
    public TraversalConfig getTraversalConfig() {return traversalConfig;}

    public GroupExtremeConfig getExtremeConfig() {return groupExtremeConfig;}

    public PgSortConfig getPgSortConfig() {return pgSortConfig;}

    public SoprpConfig getSoprpConfig() {return soprpConfig;}

    private v1HLLConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        aggConfig=builder.aggConfig;
        permuteConfig = builder.permuteConfig;
        groupExtremeConfig = builder.groupExtremeConfig;
        pgSortConfig = builder.pgSortConfig;
        soprpConfig= builder.soprpConfig;
        traversalConfig=builder.traversalConfig;
    }


    public static class Builder implements org.apache.commons.lang3.builder.Builder<v1HLLConfig> {


        private final boolean malicious;

        private final PermuteConfig permuteConfig;

        private final AggConfig aggConfig;

        private final GroupExtremeConfig groupExtremeConfig;

        private final TraversalConfig traversalConfig;
        private PgSortConfig pgSortConfig;

        private final SoprpConfig soprpConfig;

        public PermuteConfig getPermuteConfig() {
            return permuteConfig;
        }
        public AggConfig getAggConfig() {
            return aggConfig;
        }

        public GroupExtremeConfig getGroupExtremeConfig() {return groupExtremeConfig;}

        public PgSortConfig getPgSortConfig() {return pgSortConfig;}

        public SoprpConfig getSoprpConfig() {return soprpConfig;}

        public Builder(boolean malicious) {
            this.malicious = malicious;
//            sortConfig = new Hzf22PgSortConfig.Builder(malicious).build();
            traversalConfig= TraversalFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            aggConfig= AggFactory.createDefaultConfig(false);
            permuteConfig = PermuteFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            groupExtremeConfig = GroupExtremeFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            pgSortConfig = PgSortFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            soprpConfig = SoprpFactory.createDefaultConfig(SecurityModel.SEMI_HONEST,64);
        }

        @Override
        public v1HLLConfig build() {
            return new v1HLLConfig(this);
        }

        public Builder setPgSortConfig(PgSortConfig sortConfig) {
            this.pgSortConfig = sortConfig;
            return this;
        }
    }

    @Override
    public HLLPtoType getPtoType() {
        return HLLPtoType.V1;
    }
}
