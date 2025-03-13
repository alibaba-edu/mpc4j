package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.hzf22;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.PkPkJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.PkPkJoinFactory.PkPkJoinPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.RandomEncodingConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.RandomEncodingFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.MergeConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.MergeFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;

/**
 * configure of HZF22 PkPk join protocol
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public class Hzf22PkPkJoinConfig extends AbstractMultiPartyPtoConfig implements PkPkJoinConfig {
    /**
     * configure of permuation
     */
    private final PermuteConfig permuteConfig;
    /**
     * config of random encoding
     */
    private final RandomEncodingConfig encodingConfig;
    /**
     * merge config
     */
    private final MergeConfig mergeConfig;
    /**
     * type of adder
     */
    private final ComparatorType comparatorType;

    private Hzf22PkPkJoinConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        permuteConfig = builder.permuteConfig;
        encodingConfig = builder.encodingConfig;
        mergeConfig = builder.mergeConfig;
        comparatorType = builder.comparatorType;
    }

    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    public RandomEncodingConfig getEncodingConfig() {
        return encodingConfig;
    }

    public MergeConfig getMergeConfig() {
        return mergeConfig;
    }

    public ComparatorType getComparatorTypes() {
        return comparatorType;
    }

    @Override
    public PkPkJoinPtoType getPkPkJoinPtoType() {
        return PkPkJoinPtoType.PK_PK_JOIN_HZF22;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hzf22PkPkJoinConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * configure of permuation
         */
        private final PermuteConfig permuteConfig;
        /**
         * config of random encoding
         */
        private final RandomEncodingConfig encodingConfig;
        /**
         * merge config
         */
        private final MergeConfig mergeConfig;
        /**
         * type of adder
         */
        private ComparatorType comparatorType;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            SecurityModel securityModel = malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST;
            permuteConfig = PermuteFactory.createDefaultConfig(securityModel);
            encodingConfig = RandomEncodingFactory.createDefaultConfig(securityModel);
            mergeConfig = MergeFactory.createDefaultConfig(securityModel);
            comparatorType = ComparatorType.TREE_COMPARATOR;
        }

        public Builder setComparatorType(ComparatorType comparatorType) {
            this.comparatorType = comparatorType;
            return this;
        }

        @Override
        public Hzf22PkPkJoinConfig build() {
            return new Hzf22PkPkJoinConfig(this);
        }
    }
}
