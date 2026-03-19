package edu.alibaba.mpc4j.work.db.sketch.GK;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.GK.v1.v1GKConfig;
import edu.alibaba.mpc4j.work.db.sketch.GK.v1.v1GKParty;

/**
 * GK factory.
 */
public class GKFactory {
    /**
     * the protocol type
     */
    public enum GKPtoType {
        /**
         * current working v1
         */
        V1
    }

    /**
     * Creates a GK party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a GK party.
     */
    public static GKParty createParty(Abb3Party abb3Party, GKConfig config) {
        return switch (config.getPtoType()) {
            case V1 -> new v1GKParty(abb3Party, (v1GKConfig) config);
            default -> throw new IllegalArgumentException("Invalid config.getPtoType() in creating SSParty");
        };
    }

    /**
     * Creates a CMS config
     *
     * @param securityModel security model
     */
    public static GKConfig createDefaultConfig(SecurityModel securityModel) {
        return new v1GKConfig.Builder(securityModel.equals(SecurityModel.SEMI_HONEST)).build();
    }

}
