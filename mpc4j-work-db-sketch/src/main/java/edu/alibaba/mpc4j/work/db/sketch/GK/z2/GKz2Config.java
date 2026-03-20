package edu.alibaba.mpc4j.work.db.sketch.GK.z2;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalFactory;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKConfig;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKFactory;

/**
 * Z2 Boolean circuit implementation configuration for GK (Greenwald-Khanna) sketch in the S³ framework.
 * 
 * This configuration class provides settings for the Z2-based GK sketch implementation,
 * which uses Boolean circuits for secure MPC computation. It includes configurations
 * for essential MPC primitives used in the GK algorithm:
 * - Oblivious permutation for secure shuffling
 * - Group sum for aggregating values
 * - Sorting for maintaining ordered tuples
 * - Traversal for prefix operations in compaction
 * 
 * Reference: "Sketch-based Secure Query Processing for Streaming Data" (S³ framework)
 */
public class GKz2Config extends AbstractMultiPartyPtoConfig implements GKConfig {
    /**
     * Configuration for oblivious permutation protocol.
     * Used for secure shuffling of sketch tuples during merge and compaction.
     */
    private final PermuteConfig permuteConfig;
    
    /**
     * Configuration for group-by-sum protocol.
     * Used for aggregating gap and delta values during merge operations.
     */
    private final GroupSumConfig groupSumConfig;
    
    /**
     * Configuration for page-sort (pgSort) protocol.
     * Used for sorting tuples by key values in the GK sketch.
     */
    private final PgSortConfig pgSortConfig;
    
    /**
     * Configuration for traversal protocol.
     * Used for prefix operations during GK compaction and delta calculation.
     */
    private final TraversalConfig traversalConfig;

    /**
     * Constructs a GKz2Config with the specified builder parameters.
     * 
     * @param builder the builder containing configuration parameters
     */
    private GKz2Config(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        permuteConfig = builder.permuteConfig;
        groupSumConfig = builder.groupSumConfig;
        pgSortConfig = builder.pgSortConfig;
        traversalConfig = builder.traversalConfig;
    }

    @Override
    public GKFactory.GKPtoType getPtoType() {
        return GKFactory.GKPtoType.Z2;
    }

    /**
     * Gets the oblivious permutation configuration.
     * 
     * @return the permutation config
     */
    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    /**
     * Gets the group sum configuration.
     * 
     * @return the group sum config
     */
    public GroupSumConfig getGroupSumConfig() {
        return groupSumConfig;
    }

    /**
     * Gets the page-sort configuration.
     * 
     * @return the pgSort config
     */
    public PgSortConfig getPgSortConfig() {
        return pgSortConfig;
    }

    /**
     * Gets the traversal configuration.
     * 
     * @return the traversal config
     */
    public TraversalConfig getTraversalConfig() {
        return traversalConfig;
    }

    /**
     * Builder class for creating GKz2Config instances.
     * 
     * This builder allows flexible configuration of all MPC primitive protocols
     * used in the Z2 GK sketch implementation.
     */
    public static class Builder implements org.apache.commons.lang3.builder.Builder<GKz2Config> {
        /**
         * Whether the protocol should be malicious-secure.
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
         * Configuration for page-sort protocol.
         */
        private PgSortConfig pgSortConfig;
        
        /**
         * Configuration for traversal protocol.
         */
        private final TraversalConfig traversalConfig;
        
        /**
         * Type of comparator used in sorting operations.
         */
        private ComparatorFactory.ComparatorType comparatorType;

        /**
         * Creates a builder with default configurations.
         * 
         * @param malicious whether to use malicious security model
         */
        public Builder(boolean malicious) {
            this.malicious = malicious;
            permuteConfig = PermuteFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            groupSumConfig = GroupSumFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            pgSortConfig = PgSortFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            traversalConfig = TraversalFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            comparatorType = ComparatorFactory.ComparatorType.TREE_COMPARATOR;
        }

        /**
         * Sets the page-sort configuration.
         * 
         * @param pgSortConfig the pgSort configuration to use
         * @return this builder for method chaining
         */
        public Builder setPgSortConfig(PgSortConfig pgSortConfig) {
            this.pgSortConfig = pgSortConfig;
            return this;
        }

        /**
         * Sets the comparator type for sorting operations.
         * 
         * @param comparatorType the comparator type to use
         * @return this builder for method chaining
         */
        public Builder setComparatorType(ComparatorFactory.ComparatorType comparatorType) {
            this.comparatorType = comparatorType;
            return this;
        }

        @Override
        public GKz2Config build() {
            return new GKz2Config(this);
        }
    }
}

