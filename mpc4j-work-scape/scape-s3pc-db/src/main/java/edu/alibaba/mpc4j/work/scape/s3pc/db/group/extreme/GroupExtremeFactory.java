package edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.hzf22.Hzf22GroupExtremeConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.hzf22.Hzf22GroupExtremeParty;

/**
 * factory for group extreme
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public class GroupExtremeFactory {
    /**
     * aggregation type
     */
    public enum ExtremeType {
        /**
         * max
         */
        MAX,
        /**
         * min
         */
        MIN
    }

    public enum GroupExtremePtoType {
        /**
         * HZF22
         */
        HZF22
    }

    /**
     * Creates a general join party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a general join party.
     */
    public static GroupExtremeParty createParty(Abb3Party abb3Party, GroupExtremeConfig config) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (config.getPtoType()) {
            case HZF22:
                return new Hzf22GroupExtremeParty(abb3Party, (Hzf22GroupExtremeConfig) config);
            default:
                throw new IllegalArgumentException("Invalid config.getPtoType() in creating GroupExtremeParty");
        }
    }

    public static GroupExtremeConfig createDefaultConfig(SecurityModel securityModel) {
        return new Hzf22GroupExtremeConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}
