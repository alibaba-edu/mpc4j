package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.CMS.v2.v2CMSConfig;
import edu.alibaba.mpc4j.work.db.sketch.CMS.v2.v2CMSParty;

/**
 * CMS factory
 */
public class CMSFactory {
    /**
     * the protocol type
     */
    public enum CMSPtoType {
        /**
         * z2 vector
         */
        CMS_V2,
    }

    /**
     * Creates a CMS party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a CMS party.
     */
    public static CMSParty createParty(Abb3Party abb3Party, CMSConfig config) {
        return switch (config.getPtoType()) {
            case CMS_V2 -> new v2CMSParty(abb3Party, (v2CMSConfig) config);
            default -> throw new IllegalArgumentException("Invalid config.getPtoType() in creating CMSParty");
        };
    }

}
