package edu.alibaba.mpc4j.work.db.sketch.SS.z2;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.db.sketch.SS.SSFactory.SSPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.hzf22.Hzf22AggConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.db.sketch.SS.SSConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.OrderSelectConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.OrderSelectFactory;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.PopConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.PopFactory;

/**
 * SpaceSaving (SS) sketch configuration for Z2 Boolean circuit implementation.
 * 
 * <p>This configuration defines all parameters for the SS protocol using Z2 circuits,
 * which is the primary implementation in the S³ framework. It includes configurations
 * for all sub-protocols required by the Merge and Query operations.
 * 
 * <p><b>Required Sub-protocols:</b>
 * <ul>
 *   <li><b>Oblivious Permutation:</b> For shuffling data during merge operations</li>
 *   <li><b>Group-by-Sum:</b> For aggregating frequencies of identical keys (step 3 of Merge)</li>
 *   <li><b>PgSort:</b> For sorting by key (step 2) and by value (step 6) in Merge</li>
 *   <li><b>Order Select:</b> For selecting top-k entries in Query and compacting in Merge</li>
 *   <li><b>Aggregation:</b> For finding minimum/maximum values during compaction</li>
 *   <li><b>Pop:</b> For removing elements from the sketch table</li>
 * </ul>
 *
 * @author Jianzhe Yu, Qi Dong
 */
public class SSz2Config extends AbstractMultiPartyPtoConfig implements SSConfig {
    /**
     * Configuration for oblivious permutation protocol.
     * 
     * <p>Used to securely shuffle data when applying sorting results,
     * ensuring that the permutation pattern is not revealed.
     */
    private final PermuteConfig permuteConfig;
    
    /**
     * Configuration for group-by-sum protocol.
     * 
     * <p>Used in Merge protocol (Algorithm 4, step 3) to aggregate frequencies
     * of identical keys after sorting. This implements the segmented prefix-sum
     * aggregation to sum values within each key group.
     */
    private final GroupSumConfig groupSumConfig;
    
    /**
     * Configuration for PgSort (parallel graph-based sorting) protocol.
     * 
     * <p>Used in Merge protocol for:
     * - Step 2: Sort by key to group identical keys together
     * - Step 6: Sort by value (frequency) to identify top s entries
     * 
     * <p>PgSort enables flexible selection of sorting algorithms for efficiency.
     */
    private final PgSortConfig pgSortConfig;
    
    /**
     * Configuration for order select protocol.
     * 
     * <p>Used to select specific ranges of elements from sorted arrays:
     * - In Merge: Select top s entries after sorting by value
     * - In Query: Select top k entries for the query result
     */
    private final OrderSelectConfig orderSelectConfig;
    
    /**
     * Configuration for aggregation protocol.
     * 
     * <p>Used for finding extreme values (minimum/maximum) during compaction.
     * This helps identify which elements to keep or remove when the sketch
     * table exceeds capacity.
     */
    private final AggConfig aggConfig;
    
    /**
     * Configuration for pop protocol.
     * 
     * <p>Used to remove elements from the sketch table during compaction
     * when maintaining the space constraint of at most s entries.
     */
    private final PopConfig popConfig;

    /**
     * Private constructor using builder pattern.
     *
     * @param builder the configuration builder
     */
    private SSz2Config(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        permuteConfig = builder.permuteConfig;
        groupSumConfig = builder.groupSumConfig;
        pgSortConfig = builder.pgSortConfig;
        orderSelectConfig = builder.orderSelectConfig;
        aggConfig = builder.aggConfig;
        popConfig = builder.popConfig;
    }

    @Override
    public SSPtoType getPtoType() {
        return SSPtoType.Z2;
    }

    /**
     * Get the oblivious permutation configuration.
     *
     * @return the permutation protocol configuration
     */
    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    /**
     * Get the group-by-sum configuration.
     *
     * @return the group sum protocol configuration
     */
    public GroupSumConfig getGroupSumConfig() {
        return groupSumConfig;
    }

    /**
     * Get the PgSort configuration.
     *
     * @return the sorting protocol configuration
     */
    public PgSortConfig getPgSortConfig() {
        return pgSortConfig;
    }

    /**
     * Get the order select configuration.
     *
     * @return the order select protocol configuration
     */
    public OrderSelectConfig getOrderSelectConfig() {return orderSelectConfig;
    }

    /**
     * Get the aggregation configuration.
     *
     * @return the aggregation protocol configuration
     */
    public AggConfig getAggConfig() {
        return aggConfig;
    }

    /**
     * Get the pop configuration.
     *
     * @return the pop protocol configuration
     */
    public PopConfig getPopConfig() {
        return popConfig;
    }

    /**
     * Builder class for creating SSz2Config instances.
     */
    public static class Builder implements org.apache.commons.lang3.builder.Builder<SSz2Config> {
        /**
         * Whether to use malicious security model.
         */
        private final boolean malicious;
        
        /**
         * Configuration for oblivious permutation protocol.
         */
        private final PermuteConfig permuteConfig;
        
        /**
         * Configuration for group-by-sum protocol.
         */
        private final GroupSumConfig groupSumConfig;
        
        /**
         * Configuration for PgSort protocol.
         */
        private PgSortConfig pgSortConfig;
        
        /**
         * Configuration for order select protocol.
         */
        private final OrderSelectConfig orderSelectConfig;
        
        /**
         * Configuration for aggregation protocol.
         */
        private AggConfig aggConfig;
        
        /**
         * Configuration for pop protocol.
         */
        private final PopConfig popConfig;
        
        /**
         * Type of comparator to use in aggregation.
         */
        private ComparatorType comparatorType;

        /**
         * Constructor for builder with default configurations.
         *
         * @param malicious whether to use malicious security model
         */
        public Builder(boolean malicious) {
            this.malicious = malicious;
            permuteConfig = PermuteFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            groupSumConfig = GroupSumFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            pgSortConfig = PgSortFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            orderSelectConfig = OrderSelectFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            comparatorType = ComparatorType.TREE_COMPARATOR;
            aggConfig = new Hzf22AggConfig.Builder(malicious).setComparatorType(comparatorType).build();
            popConfig = PopFactory.createDefaultConfig(malicious);
        }

        /**
         * Set the PgSort configuration.
         *
         * @param pgSortConfig the sorting protocol configuration
         * @return this builder for chaining
         */
        public Builder setPgSortConfig(PgSortConfig pgSortConfig) {
            this.pgSortConfig = pgSortConfig;
            return this;
        }

        /**
         * Set the comparator type for aggregation.
         *
         * @param comparatorType the comparator type (e.g., TREE_COMPARATOR)
         * @return this builder for chaining
         */
        public Builder setComparatorType(ComparatorType comparatorType) {
            this.comparatorType = comparatorType;
            aggConfig = new Hzf22AggConfig.Builder(malicious).setComparatorType(comparatorType).build();
            return this;
        }

        /**
         * Build the SSz2Config instance.
         *
         * @return the constructed configuration
         */
        @Override
        public SSz2Config build() {
            return new SSz2Config(this);
        }
    }
}
