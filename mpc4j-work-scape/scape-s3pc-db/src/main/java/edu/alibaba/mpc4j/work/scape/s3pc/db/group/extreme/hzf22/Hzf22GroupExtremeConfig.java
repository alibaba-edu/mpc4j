package edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.hzf22;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeFactory.GroupExtremePtoType;

/**
 * Configure of HZF22 group extreme protocol
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public class Hzf22GroupExtremeConfig extends AbstractMultiPartyPtoConfig implements GroupExtremeConfig {
    /**
     * type of adder
     */
    private final ComparatorType comparatorType;

    private Hzf22GroupExtremeConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        comparatorType = builder.comparatorType;
    }

    @Override
    public GroupExtremePtoType getPtoType() {
        return GroupExtremePtoType.HZF22;
    }

    @Override
    public ComparatorType getComparatorTypes() {
        return comparatorType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hzf22GroupExtremeConfig> {
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
        public Hzf22GroupExtremeConfig build() {
            return new Hzf22GroupExtremeConfig(this);
        }
    }
}
