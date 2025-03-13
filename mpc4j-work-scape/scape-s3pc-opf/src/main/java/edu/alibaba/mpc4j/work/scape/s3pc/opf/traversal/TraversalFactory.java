package edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.hzf22.Hzf22TraversalConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.hzf22.Hzf22TraversalParty;

/**
 * TraversalFactory
 *
 * @author Feng Han
 * @date 2025/2/14
 */
public class TraversalFactory {
    /**
     * the protocol type
     */
    public enum TraversalType {
        /**
         * Scape HZF22
         */
        TRAVERSAL_HZF22,
    }

    /**
     * Creates a traversal party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a z2c party.
     */
    public static TraversalParty createParty(Abb3Party abb3Party, TraversalConfig config) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (config.getTraversalType()) {
            case TRAVERSAL_HZF22:
                return new Hzf22TraversalParty(abb3Party, (Hzf22TraversalConfig) config);
            default:
                throw new IllegalArgumentException("Invalid config.getTraversalType() in creating TraversalParty");
        }
    }

    public static TraversalConfig createDefaultConfig(SecurityModel securityModel) {
        return new Hzf22TraversalConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}
