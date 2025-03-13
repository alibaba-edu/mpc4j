package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.kks20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPermutationConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPermutationFactory.FillType;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix.RadixPgSortConfig;

/**
 * configure of the protocol which fills the incomplete permutation using butterfly net
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public class Kks20FillPermutationConfig extends AbstractMultiPartyPtoConfig implements FillPermutationConfig {
    /**
     * config of pg sorter
     */
    private final PgSortConfig pgSortConfig;
    /**
     * configure of permuation
     */
    private final PermuteConfig permuteConfig;

    private Kks20FillPermutationConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        pgSortConfig = builder.pgSortConfig;
        permuteConfig = builder.permuteConfig;
    }

    public PgSortConfig getPgSortConfig() {
        return pgSortConfig;
    }

    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    @Override
    public FillType getFillType() {
        return FillType.BUTTERFLY_NET_KKS20;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kks20FillPermutationConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * config of pg sorter
         */
        private final PgSortConfig pgSortConfig;
        /**
         * configure of permuation
         */
        private final PermuteConfig permuteConfig;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            SecurityModel securityModel = malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST;
            pgSortConfig = new RadixPgSortConfig.Builder(malicious).build();
            permuteConfig = PermuteFactory.createDefaultConfig(securityModel);
        }

        @Override
        public Kks20FillPermutationConfig build() {
            return new Kks20FillPermutationConfig(this);
        }
    }
}