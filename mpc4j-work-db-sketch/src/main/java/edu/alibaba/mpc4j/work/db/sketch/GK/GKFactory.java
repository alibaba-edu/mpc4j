package edu.alibaba.mpc4j.work.db.sketch.GK;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.GK.z2.GKz2Config;
import edu.alibaba.mpc4j.work.db.sketch.GK.z2.GKz2Party;

/**
 * GK (Greenwald-Khanna) factory in the S³ framework.
 * 
 * This factory class is responsible for creating GK party instances with
 * specific configurations. It supports different protocol implementations,
 * currently including Z2 Boolean circuit implementation.
 * 
 * The factory pattern allows for easy extension with new protocol types
 * and provides a centralized way to configure GK sketch instances.
 */
public class GKFactory {
    /**
     * The protocol type enum for GK sketch implementations.
     * 
     * Currently supports:
     * - Z2: Boolean circuit implementation for MPC
     */
    public enum GKPtoType {
        /**
         * Z2 Boolean circuit implementation (current working version v1)
         * Uses Z2 arithmetic for secure computation of GK sketch operations
         */
        Z2
    }

    /**
     * Creates a GK party instance based on the provided configuration.
     * 
     * This factory method instantiates the appropriate GK party implementation
     * based on the protocol type specified in the configuration.
     *
     * @param abb3Party the ABB3 party providing the underlying MPC primitives
     * @param config    the GK configuration specifying protocol type and parameters
     * @return a GK party instance ready for secure sketch operations
     * @throws IllegalArgumentException if the protocol type is invalid
     */
    public static GKParty createParty(Abb3Party abb3Party, GKConfig config) {
        return switch (config.getPtoType()) {
            case Z2 -> new GKz2Party(abb3Party, (GKz2Config) config);
            default -> throw new IllegalArgumentException("Invalid config.getPtoType() in creating SSParty");
        };
    }

    /**
     * Creates a default GK configuration for the specified security model.
     * 
     * @param securityModel the security model (SEMI_HONEST or MALICIOUS)
     * @return a GK configuration with default settings for the specified security model
     */
    public static GKConfig createDefaultConfig(SecurityModel securityModel) {
        return new GKz2Config.Builder(securityModel.equals(SecurityModel.SEMI_HONEST)).build();
    }

}
