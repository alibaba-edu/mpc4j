package edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22.Hzf22GroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22.Hzf22GroupSumParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22ext.Hzf22ExtGroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22ext.Hzf22ExtGroupSumParty;

/**
 * factory for group sum.
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public class GroupSumFactory {

    public enum GroupSumPtoType {
        /**
         * HZF22
         */
        HZF22,
        /**
         * HZF22EXT, the method uses oblivious permutation
         */
        HZF22EXT
    }

    /**
     * Creates a general join party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a general join party.
     */
    public static GroupSumParty createParty(Abb3Party abb3Party, GroupSumConfig config) {
        return switch (config.getPtoType()) {
            case HZF22 -> new Hzf22GroupSumParty(abb3Party, (Hzf22GroupSumConfig) config);
            case HZF22EXT -> new Hzf22ExtGroupSumParty(abb3Party, (Hzf22ExtGroupSumConfig) config);
            default -> throw new IllegalArgumentException("Invalid config.getPtoType() in creating GroupSumParty");
        };
    }

    public static GroupSumConfig createDefaultConfig(SecurityModel securityModel) {
        return new Hzf22GroupSumConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}
