package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.hzf22.Hzf22PkPkJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.hzf22.Hzf22PkPkJoinParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.mrr20.Mrr20PkPkJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.mrr20.Mrr20PkPkJoinParty;

/**
 * PkPk Join Factory
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public class PkPkJoinFactory implements PtoFactory {
    public enum PkPkJoinPtoType{
        /**
         * HZF22 PkPk Join
         */
        PK_PK_JOIN_HZF22,
        /**
         * MRR20 PkPk Join
         */
        PK_PK_JOIN_MRR20,
    }

    /**
     * Creates a general join party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a general join party.
     */
    public static PkPkJoinParty createParty(Abb3Party abb3Party, PkPkJoinConfig config) {
        switch (config.getPkPkJoinPtoType()) {
            case PK_PK_JOIN_MRR20:
                return new Mrr20PkPkJoinParty(abb3Party, (Mrr20PkPkJoinConfig) config);
            case PK_PK_JOIN_HZF22:
                return new Hzf22PkPkJoinParty(abb3Party, (Hzf22PkPkJoinConfig) config);
            default:
                throw new IllegalArgumentException("Invalid config.getPkPkJoinPtoType() in creating PkPkJoinParty");
        }
    }

    public static PkPkJoinConfig createDefaultConfig(SecurityModel securityModel) {
        return new Mrr20PkPkJoinConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}
