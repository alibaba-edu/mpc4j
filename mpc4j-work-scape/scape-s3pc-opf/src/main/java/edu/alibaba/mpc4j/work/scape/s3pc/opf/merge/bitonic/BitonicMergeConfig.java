package edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.bitonic;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.MergeConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.MergeFactory.MergeType;

/**
 * configuration of bitonic merge protocol
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public class BitonicMergeConfig extends AbstractMultiPartyPtoConfig implements MergeConfig {
    /**
     * type of adder
     */
    private final ComparatorType comparatorType;

    private BitonicMergeConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        comparatorType = builder.comparatorType;
    }

    @Override
    public MergeType getMergeType() {
        return MergeType.MERGE_BITONIC;
    }

    public ComparatorType getComparatorTypes() {
        return comparatorType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<BitonicMergeConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * type of adder
         */
        private ComparatorType comparatorType;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            comparatorType = ComparatorType.TREE_COMPARATOR;
        }

        public Builder setComparatorType(ComparatorType comparatorType) {
            this.comparatorType = comparatorType;
            return this;
        }

        @Override
        public BitonicMergeConfig build() {
            return new BitonicMergeConfig(this);
        }
    }
}
