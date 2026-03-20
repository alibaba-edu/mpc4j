package edu.alibaba.mpc4j.work.db.sketch.CMS.z2;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.db.sketch.CMS.CMSConfig;
import edu.alibaba.mpc4j.work.db.sketch.CMS.CMSFactory;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.TruncateConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.TruncateFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22ext.Hzf22ExtGroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpFactory;

/**
 * Configuration for the Z2 Boolean circuit implementation of Count-Min Sketch (CMS) in the S³ framework.
 * 
 * <p>This class configures the CMSz2Party protocol, which implements secure CMS operations
 * (update and query) using Z2 Boolean circuits. The protocol relies on several oblivious
 * primitives for secure computation:</p>
 * 
 * <p>Key sub-protocols:
 * - SOPRP (Secure Oblivious Permutation with Random Permutation): for oblivious hash computation
 * - PgSort: for oblivious sorting in the Merge protocol
 * - Oblivious Permutation: for applying and inverting permutations
 * - Group-by-Sum: for segmented prefix-sum in the Merge protocol
 * - Truncate: for compacting the merged results</p>
 * 
 * <p>The configuration supports both semi-honest and malicious security models.</p>
 */
public class CMSz2Config extends AbstractMultiPartyPtoConfig implements CMSConfig {
    /**
     * Configuration for oblivious permutation protocol.
     * 
     * <p>Used in the Merge protocol to apply and invert permutations on sorted data.</p>
     */
    private final PermuteConfig permuteConfig;
    /**
     * Configuration for group-by-sum protocol.
     * 
     * <p>Used in the Merge protocol for segmented prefix-sum computation,
     * which aggregates counts for identical hash indices.</p>
     */
    private final GroupSumConfig groupSumConfig;
    /**
     * Configuration for PgSort oblivious sorting protocol.
     * 
     * <p>Used in the Merge protocol to sort buffer data by hash indices,
     * enabling efficient group-by-sum operations.</p>
     */
    private final PgSortConfig pgSortConfig;
    /**
     * Configuration for SOPRP (Secure Oblivious Permutation with Random Permutation) protocol.
     * 
     * <p>Used for oblivious hash computation, mapping keys to indices
     * without revealing the key-index relationship.</p>
     */
    private final SoprpConfig soprpConfig;
    /**
     * Configuration for truncation protocol.
     * 
     * <p>Used in the Merge protocol to compact merged results and
     * truncate to the top s values for the sketch table.</p>
     */
    private final TruncateConfig truncateConfig;

    /**
     * Private constructor for CMSz2Config.
     * 
     * @param builder the builder containing all configuration parameters
     */
    private CMSz2Config(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        permuteConfig = builder.permuteConfig;
        groupSumConfig = builder.groupSumConfig;
        pgSortConfig = builder.pgSortConfig;
        soprpConfig = builder.soprpConfig;
        truncateConfig = builder.truncateConfig;
    }

    /**
     * Gets the protocol type for this configuration.
     * 
     * @return CMS_Z2, indicating Z2 Boolean circuit implementation
     */
    @Override
    public CMSFactory.CMSPtoType getPtoType() {
        return CMSFactory.CMSPtoType.CMS_Z2;
    }

    /**
     * Gets the oblivious permutation configuration.
     * 
     * @return the PermuteConfig for oblivious permutation operations
     */
    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    /**
     * Gets the group-by-sum configuration.
     * 
     * @return the GroupSumConfig for segmented prefix-sum operations
     */
    public GroupSumConfig getGroupSumConfig() {
        return groupSumConfig;
    }

    /**
     * Gets the PgSort configuration.
     * 
     * @return the PgSortConfig for oblivious sorting operations
     */
    public PgSortConfig getPgSortConfig() {
        return pgSortConfig;
    }

    /**
     * Gets the SOPRP configuration.
     * 
     * @return the SoprpConfig for oblivious hash computation
     */
    public SoprpConfig getSoprpConfig() {
        return soprpConfig;
    }
    /**
     * Gets the truncation configuration.
     * 
     * @return the TruncateConfig for truncation operations
     */
    public TruncateConfig getTruncateConfig() {
        return truncateConfig;
    }

    /**
     * Builder class for constructing CMSz2Config instances.
     * 
     * <p>The builder provides default configurations for all sub-protocols
     * and allows customization of specific parameters.</p>
     */
    public static class Builder implements org.apache.commons.lang3.builder.Builder<CMSz2Config> {
        /**
         * Whether to use malicious security model.
         * 
         * <p>If true, the protocol protects against malicious adversaries;
         * if false, it protects against semi-honest adversaries.</p>
         */
        private final boolean malicious;
        /**
         * Configuration for oblivious permutation.
         */
        private final PermuteConfig permuteConfig;
        /**
         * Configuration for group-by-sum.
         */
        private final GroupSumConfig groupSumConfig;

        private final SoprpConfig soprpConfig;
        /**
         * Configuration for PgSort oblivious sorting.
         */
        PgSortConfig pgSortConfig;
        /**
         * Configuration for truncation.
         */
        private final TruncateConfig truncateConfig;

        /**
         * Constructs a Builder with the specified security model.
         * 
         * <p>Initializes all sub-protocol configurations with default values
         * appropriate for the specified security model.</p>
         * 
         * @param malicious whether to use malicious security model
         */
        public Builder(boolean malicious) {
            this.malicious = malicious;
            permuteConfig = PermuteFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            groupSumConfig = new Hzf22ExtGroupSumConfig.Builder(malicious).build();
            pgSortConfig = PgSortFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            soprpConfig = SoprpFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, 64);
            truncateConfig = TruncateFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        /**
         * Sets a custom PgSort configuration.
         * 
         * @param pgSortConfig the custom PgSort configuration
         * @return this builder for method chaining
         */
        public Builder setPgSortConfig(PgSortConfig pgSortConfig) {
            this.pgSortConfig = pgSortConfig;
            return this;
        }

        /**
         * Builds the CMSz2Config with the current settings.
         * 
         * @return a new CMSz2Config instance
         */
        @Override
        public CMSz2Config build() {
            return new CMSz2Config(this);
        }
    }
}