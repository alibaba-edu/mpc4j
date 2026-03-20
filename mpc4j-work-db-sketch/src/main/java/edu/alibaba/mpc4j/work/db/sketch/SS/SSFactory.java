package edu.alibaba.mpc4j.work.db.sketch.SS;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.SS.z2.SSz2Config;
import edu.alibaba.mpc4j.work.db.sketch.SS.z2.SSz2Party;

/**
 * SpaceSaving (SS) sketch factory for creating protocol instances.
 * 
 * <p>This factory is responsible for creating SS sketch protocol parties and configurations
 * based on the specified protocol type and security model. It serves as the entry point
 * for instantiating SS sketch implementations in the S³ framework.
 */
public class SSFactory {
    /**
     * The available protocol types for SS sketch implementation.
     */
    public enum SSPtoType {
        /**
         * Z2 Boolean circuit implementation - current working implementation
         * Uses Z2 arithmetic for secure computation of SS operations
         */
        Z2,
    }

    /**
     * Creates an SS sketch computing party based on the provided configuration.
     * 
     * <p>This factory method instantiates the appropriate SS party implementation
     * (currently only Z2 is supported) with the given ABB3 party and configuration.
     *
     * @param abb3Party the ABB3 (3-party arithmetic) party for MPC operations
     * @param config    the SS protocol configuration specifying implementation type
     * @return an SS sketch party instance ready for secure computation
     */
    public static SSParty createParty(Abb3Party abb3Party, SSConfig config) {
        return switch (config.getPtoType()) {
            case Z2 -> new SSz2Party(abb3Party, (SSz2Config) config);
            default -> throw new IllegalArgumentException("Invalid config.getPtoType() in creating SSParty");
        };
    }

    /**
     * Creates a default SS sketch configuration for the specified security model.
     * 
     * <p>This method creates a configuration with sensible default parameters
     * for the given security model (semi-honest or malicious).
     *
     * @param securityModel the security model (SEMI_HONEST or MALICIOUS)
     * @return a default SS configuration instance
     */
    public static SSConfig createDefaultConfig(SecurityModel securityModel) {
        return new SSz2Config.Builder(securityModel.equals(SecurityModel.SEMI_HONEST)).build();
    }
}
