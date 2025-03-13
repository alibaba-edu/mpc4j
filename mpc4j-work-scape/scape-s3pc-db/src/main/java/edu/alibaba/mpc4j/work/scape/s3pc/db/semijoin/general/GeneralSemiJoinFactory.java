package edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general.hzf22.Hzf22GeneralSemiJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general.hzf22.Hzf22GeneralSemiJoinParty;

/**
 * factory for general semi-join
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public class GeneralSemiJoinFactory implements PtoFactory {
    /**
     * protocol type
     */
    public enum GeneralSemiJoinPtoType {
        /**
         * hzf22 general semi-join
         */
        GENERAL_SEMI_JOIN_HZF22,
    }

    /**
     * Creates a general join party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a general join party.
     */
    public static GeneralSemiJoinParty createParty(Abb3Party abb3Party, GeneralSemiJoinConfig config) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (config.getGeneralSemiJoinPtoType()) {
            case GENERAL_SEMI_JOIN_HZF22:
                return new Hzf22GeneralSemiJoinParty(abb3Party, (Hzf22GeneralSemiJoinConfig) config);
            default:
                throw new IllegalArgumentException("Invalid config.getGeneralJoinPtoType() in creating GeneralJoinParty");
        }
    }

    public static GeneralSemiJoinConfig createDefaultConfig(SecurityModel securityModel) {
        return new Hzf22GeneralSemiJoinConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}
