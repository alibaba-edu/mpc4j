package edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.hzf22.Hzf22PkPkSemiJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.hzf22.Hzf22PkPkSemiJoinParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.mrr20.Mrr20PkPkSemiJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.mrr20.Mrr20PkPkSemiJoinParty;

/**
 * PkPk semi-join factory
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public class PkPkSemiJoinFactory implements PtoFactory {
    public enum PkPkSemiJoinPtoType {
        /**
         * HZF22
         */
        PK_PK_SEMI_JOIN_HZF22,
        /**
         * Mrr20
         */
        PK_PK_SEMI_JOIN_MRR20,
    }

    /**
     * Creates a general join party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a general join party.
     */
    public static PkPkSemiJoinParty createParty(Abb3Party abb3Party, PkPkSemiJoinConfig config) {
        return switch (config.getPkPkSemiJoinPtoType()) {
            case PK_PK_SEMI_JOIN_MRR20 -> new Mrr20PkPkSemiJoinParty(abb3Party, (Mrr20PkPkSemiJoinConfig) config);
            case PK_PK_SEMI_JOIN_HZF22 -> new Hzf22PkPkSemiJoinParty(abb3Party, (Hzf22PkPkSemiJoinConfig) config);
            default -> throw new IllegalArgumentException("Invalid config.getPkPkSjPtoType() in creating PkPkSjParty");
        };
    }

    public static PkPkSemiJoinConfig createDefaultConfig(SecurityModel securityModel) {
        return new Mrr20PkPkSemiJoinConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}
