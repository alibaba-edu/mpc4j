package edu.alibaba.mpc4j.work.db.sketch.utils.pop;

import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.naive.NaivePopConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.naive.NaivePopParty;

/**
 * Factory for creating Pop (Permute-and-Open) protocol instances.
 * Provides methods to create Pop parties and configurations with different implementations.
 */
public class PopFactory {
    /**
     * Enumeration of available Pop protocol types
     */
    public enum PopPtoType {
        /**
         * NAIVE implementation - straightforward approach using basic circuit operations
         */
        NAIVE
    }

    /**
     * Creates a Pop party instance based on the provided configuration
     *
     * @param abb3Party the underlying ABB3 party for three-party computation
     * @param config    the protocol configuration
     * @return a new Pop party instance
     */
    public static PopParty createParty(Abb3Party abb3Party, PopConfig config) {
        // noinspection EnhancedSwitchStatement
        switch (config.getPtoType()) {
            case NAIVE:
                return new NaivePopParty(abb3Party, (NaivePopConfig) config);
            default:
                throw new IllegalArgumentException("Invalid PopPtoType: " + config.getPtoType().name());
        }
    }

    /**
     * Creates a default Pop configuration
     *
     * @param malicious whether to use malicious security model
     * @return a default Pop configuration
     */
    public static PopConfig createDefaultConfig(boolean malicious) {
        return new NaivePopConfig.Builder(malicious).build();
    }
}
