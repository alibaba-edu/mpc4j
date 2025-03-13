package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.hzf22.Hzf22SortSignConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.hzf22.Hzf22SortSignParty;

/**
 * SortSign Factory
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public class SortSignFactory {
    /**
     * sortSign type
     */
    public enum SortSignType{
        /**
         * HZF22
         */
        HZF22,
    }

    /**
     * Creates a permutation generation sorting party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a permutation generation sorting party.
     */
    public static SortSignParty createParty(Abb3Party abb3Party, SortSignConfig config) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (config.getSortSignType()) {
            case HZF22:
                return new Hzf22SortSignParty(abb3Party, (Hzf22SortSignConfig) config);
            default:
                throw new IllegalArgumentException("Invalid config.getSortSignType() in creating SortSignParty");
        }
    }

    public static SortSignConfig createDefaultConfig(SecurityModel securityModel) {
        return new Hzf22SortSignConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}
