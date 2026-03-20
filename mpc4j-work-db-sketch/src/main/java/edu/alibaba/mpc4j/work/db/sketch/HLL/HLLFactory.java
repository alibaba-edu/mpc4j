package edu.alibaba.mpc4j.work.db.sketch.HLL;

import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.HLL.z2.HLLz2Config;
import edu.alibaba.mpc4j.work.db.sketch.HLL.z2.HLLz2Party;

/**
 * Factory class for creating HyperLogLog (HLL) parties in the S³ Framework.
 * 
 * This factory provides a centralized way to instantiate HLL party implementations
 * based on the specified configuration. It supports different protocol types
 * for various security models and performance requirements.
 * 
 * Currently supported protocol types:
 * - Z2: Boolean circuit-based implementation using ABB3 framework
 */
public class HLLFactory {
    /**
     * Enumeration of supported HLL protocol types.
     * Each type represents a different implementation approach for secure computation.
     */
    public enum HLLPtoType {
        /**
         * Z2 Boolean circuit version.
         * Implements HLL using Z2 Boolean circuits with ABB3 framework.
         * This is the v1 HLL implementation focusing on efficient Boolean circuit operations.
         */
        Z2,
    }

    /**
     * Creates an HLL party based on the specified configuration.
     * 
     * This factory method instantiates the appropriate HLL party implementation
     * based on the protocol type specified in the configuration.
     * 
     * @param abb3Party the ABB3 party instance for underlying secure computation primitives
     * @param config the HLL configuration specifying the protocol type and parameters
     * @return an HLL party instance ready for secure computation
     * @throws IllegalArgumentException if an invalid protocol type is specified
     */
    public static HLLParty createHLLParty(Abb3Party abb3Party, HLLConfig config) {
        return switch (config.getPtoType()) {
            case Z2 -> new HLLz2Party(abb3Party, (HLLz2Config) config);
            default -> throw new IllegalArgumentException("Invalid config.getPtoType() in creating HLLParty");
        };
    }
}
