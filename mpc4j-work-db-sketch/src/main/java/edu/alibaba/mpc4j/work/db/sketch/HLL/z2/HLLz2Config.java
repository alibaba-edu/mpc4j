package edu.alibaba.mpc4j.work.db.sketch.HLL.z2;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.db.sketch.HLL.HLLConfig;
import edu.alibaba.mpc4j.work.db.sketch.HLL.HLLFactory.HLLPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalFactory;

/**
 * Configuration for Z2 Boolean circuit-based HyperLogLog (HLL) protocol in the S³ Framework.
 *
 * This class defines the configuration parameters for HLL implementation using Z2 Boolean circuits.
 * It specifies configurations for various sub-protocols used in the HLL merge and query operations:
 * - Permute: For applying permutations during sorting and compaction
 * - Agg: For aggregation operations
 * - Traversal: For prefix operations to compute leading ones
 * - GroupExtreme: For segmented prefix-max operation during merge
 * - PgSort: For sorting buffered elements by hash index
 * - Soprp (LowMC): For secure hash computation
 *
 * The merge protocol uses these sub-protocols to implement:
 * 1. Hash computation (LowMC)
 * 2. Sorting by h1(k) (PgSort)
 * 3. Segmented prefix-max (GroupExtreme)
 * 4. Compaction (Permute)
 */
public class HLLz2Config extends AbstractMultiPartyPtoConfig implements HLLConfig {
    /**
     * Configuration for permutation operations.
     * Used for applying inverse permutations during sorting and compaction steps.
     */
    private final PermuteConfig permuteConfig;

    /**
     * Configuration for aggregation operations.
     * Used for summing counters during query operation.
     */
    private final AggConfig aggConfig;

    /**
     * Configuration for traversal operations.
     * Used for prefix-and operations to compute leading ones count.
     * The leading ones computation uses prefix-and circuit + sum as described in the paper.
     */
    private final TraversalConfig traversalConfig;

    /**
     * Configuration for group extreme operations.
     * Used for segmented prefix-max during merge protocol.
     * Groups entries by h1(k) and computes max of LeadingOnes(h2(k)) within each group.
     */
    private final GroupExtremeConfig groupExtremeConfig;

    /**
     * Configuration for page-based sorting operations.
     * Used to sort buffered elements by their hash index h1(k) before merging.
     * Enables flexible selection of sorting algorithms for optimal performance.
     */
    private final PgSortConfig pgSortConfig;

    /**
     * Configuration for secure oblivious permutation (SOPRP) using LowMC cipher.
     * LowMC is used as the hash function to compute h1(k) and h2(k) securely.
     */
    private final SoprpConfig soprpConfig;

    /**
     * Gets the permutation configuration.
     *
     * @return the permutation configuration
     */
    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    /**
     * Gets the aggregation configuration.
     *
     * @return the aggregation configuration
     */
    public AggConfig getAggConfig() {
        return aggConfig;
    }

    /**
     * Gets the traversal configuration.
     *
     * @return the traversal configuration
     */
    public TraversalConfig getTraversalConfig() {
        return traversalConfig;
    }

    /**
     * Gets the group extreme configuration.
     *
     * @return the group extreme configuration
     */
    public GroupExtremeConfig getExtremeConfig() {
        return groupExtremeConfig;
    }

    /**
     * Gets the page-based sort configuration.
     *
     * @return the page-based sort configuration
     */
    public PgSortConfig getPgSortConfig() {
        return pgSortConfig;
    }

    /**
     * Gets the SOPRP (LowMC) configuration.
     *
     * @return the SOPRP configuration
     */
    public SoprpConfig getSoprpConfig() {
        return soprpConfig;
    }

    /**
     * Private constructor for HLLz2Config.
     *
     * @param builder the builder containing all configuration parameters
     */
    private HLLz2Config(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        aggConfig = builder.aggConfig;
        permuteConfig = builder.permuteConfig;
        groupExtremeConfig = builder.groupExtremeConfig;
        pgSortConfig = builder.pgSortConfig;
        soprpConfig = builder.soprpConfig;
        traversalConfig = builder.traversalConfig;
    }

    /**
     * Builder class for constructing HLLz2Config instances.
     * Uses the builder pattern to allow flexible configuration of all sub-protocols.
     */
    public static class Builder implements org.apache.commons.lang3.builder.Builder<HLLz2Config> {

        /**
         * Security model flag: true for malicious, false for semi-honest.
         */
        private final boolean malicious;

        /**
         * Permutation configuration.
         */
        private final PermuteConfig permuteConfig;

        /**
         * Aggregation configuration.
         */
        private final AggConfig aggConfig;

        /**
         * Group extreme configuration.
         */
        private final GroupExtremeConfig groupExtremeConfig;

        /**
         * Traversal configuration.
         */
        private final TraversalConfig traversalConfig;

        /**
         * Page-based sort configuration (mutable).
         */
        private PgSortConfig pgSortConfig;

        /**
         * SOPRP (LowMC) configuration.
         */
        private final SoprpConfig soprpConfig;

        /**
         * Gets the permutation configuration.
         *
         * @return the permutation configuration
         */
        public PermuteConfig getPermuteConfig() {
            return permuteConfig;
        }

        /**
         * Gets the aggregation configuration.
         *
         * @return the aggregation configuration
         */
        public AggConfig getAggConfig() {
            return aggConfig;
        }

        /**
         * Gets the group extreme configuration.
         *
         * @return the group extreme configuration
         */
        public GroupExtremeConfig getGroupExtremeConfig() {
            return groupExtremeConfig;
        }

        /**
         * Gets the page-based sort configuration.
         *
         * @return the page-based sort configuration
         */
        public PgSortConfig getPgSortConfig() {
            return pgSortConfig;
        }

        /**
         * Gets the SOPRP configuration.
         *
         * @return the SOPRP configuration
         */
        public SoprpConfig getSoprpConfig() {
            return soprpConfig;
        }

        /**
         * Constructs a Builder with the specified security model.
         * Initializes all sub-protocol configurations with default values.
         *
         * @param malicious true for malicious security model, false for semi-honest
         */
        public Builder(boolean malicious) {
            this.malicious = malicious;
//            sortConfig = new Hzf22PgSortConfig.Builder(malicious).build();
            traversalConfig = TraversalFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            aggConfig = AggFactory.createDefaultConfig(false);
            permuteConfig = PermuteFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            groupExtremeConfig = GroupExtremeFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            pgSortConfig = PgSortFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            // LowMC with 64-bit block size for hash computation
            soprpConfig = SoprpFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, 64);
        }

        /**
         * Builds the HLLz2Config instance.
         *
         * @return the constructed HLLz2Config
         */
        @Override
        public HLLz2Config build() {
            return new HLLz2Config(this);
        }

        /**
         * Sets a custom page-based sort configuration.
         *
         * @param sortConfig the custom sort configuration
         * @return this builder for method chaining
         */
        public Builder setPgSortConfig(PgSortConfig sortConfig) {
            this.pgSortConfig = sortConfig;
            return this;
        }
    }

    /**
     * Gets the protocol type.
     *
     * @return HLLPtoType.Z2 indicating Z2 Boolean circuit implementation
     */
    @Override
    public HLLPtoType getPtoType() {
        return HLLPtoType.Z2;
    }
}

