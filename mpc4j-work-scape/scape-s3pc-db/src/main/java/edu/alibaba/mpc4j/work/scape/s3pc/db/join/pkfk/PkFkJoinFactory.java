package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk.hzf22.Hzf22PkFkJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk.hzf22.Hzf22PkFkJoinParty;

/**
 * Factory for PkFk Join.
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public class PkFkJoinFactory implements PtoFactory {
    public enum PkFkJoinPtoType{
        PK_FK_JOIN_HZF22,
        PK_FK_JOIN_AHK23,
    }

    /**
     * Creates a general join party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a general join party.
     */
    public static PkFkJoinParty createParty(Abb3Party abb3Party, PkFkJoinConfig config) {
        switch (config.getPkFkJoinPtoType()) {
            case PK_FK_JOIN_HZF22:
                return new Hzf22PkFkJoinParty(abb3Party, (Hzf22PkFkJoinConfig) config);
            case PK_FK_JOIN_AHK23:
            default:
                throw new IllegalArgumentException("Invalid config.getPkFkJoinPtoType() in creating PkFkJoinParty");
        }
    }

    public static PkFkJoinConfig createDefaultConfig(SecurityModel securityModel) {
        return new Hzf22PkFkJoinConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}
