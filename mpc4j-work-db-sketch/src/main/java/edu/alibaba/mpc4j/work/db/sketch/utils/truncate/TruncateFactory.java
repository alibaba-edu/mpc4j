package edu.alibaba.mpc4j.work.db.sketch.utils.truncate;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.ext.ExtTruncateConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.ext.ExtTruncateParty;

/**
 * Factory for creating Truncate protocol instances.
 * Provides methods to create truncate parties and configurations with different implementations.
 */
public class TruncateFactory {
    /**
     * Enumeration of available Truncate protocol types
     */
    public enum TruncatePtoType {
        /**
         * EXT implementation - uses oblivious permutation for efficient truncation
         */
        EXT
    }

    /**
     * Creates a Truncate party instance based on the provided configuration
     *
     * @param config    the protocol configuration
     * @param abb3Party the underlying ABB3 party for three-party computation
     * @return a new Truncate party instance
     */
    public static TruncateParty createParty(Abb3Party abb3Party, TruncateConfig config) {
        return switch (config.getPtoType()) {
            case EXT -> new ExtTruncateParty(abb3Party, (ExtTruncateConfig) config);
            default -> throw new IllegalArgumentException("Invalid config.getPtoType() in creating TruncateParty");
        };
    }

    /**
     * Creates a default Truncate configuration
     *
     * @param securityModel the security model to use (MALICIOUS or SEMI_HONEST)
     * @return a default Truncate configuration
     */
    public static TruncateConfig createDefaultConfig(SecurityModel securityModel) {
        return new ExtTruncateConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}
