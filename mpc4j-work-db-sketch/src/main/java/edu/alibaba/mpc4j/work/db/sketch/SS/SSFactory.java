package edu.alibaba.mpc4j.work.db.sketch.SS;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.SS.v1.v1SSConfig;
import edu.alibaba.mpc4j.work.db.sketch.SS.v1.v1SSParty;

/**
 * MG factory.
 */
public class SSFactory {
    /**
     * the protocol type
     */
    public enum MGPtoType {
        /**
         * current working v1
         */
        V1,
        /**
         * Naive baseline
         */
        BK21
    }

    /**
     * Creates a CMS party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a CMS party.
     */
    public static SSParty createParty(Abb3Party abb3Party, SSConfig config) {
        return switch (config.getPtoType()) {
            case V1 -> new v1SSParty(abb3Party, (v1SSConfig) config);
            default -> throw new IllegalArgumentException("Invalid config.getPtoType() in creating SSParty");
        };
    }

    /**
     * Creates a CMS config
     *
     * @param securityModel security model
     */
    public static SSConfig createDefaultConfig(SecurityModel securityModel) {
        return new v1SSConfig.Builder(securityModel.equals(SecurityModel.SEMI_HONEST)).build();
    }
}
