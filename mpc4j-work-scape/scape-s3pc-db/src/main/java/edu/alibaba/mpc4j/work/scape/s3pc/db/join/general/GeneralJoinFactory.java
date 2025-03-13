package edu.alibaba.mpc4j.work.scape.s3pc.db.join.general;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.general.hzf22.Hzf22GeneralJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.general.hzf22.Hzf22GeneralJoinParty;

/**
 * general Join Factory
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public class GeneralJoinFactory implements PtoFactory {
    public enum GeneralJoinPtoType{
        GENERAL_JOIN_HZF22,
    }

    /**
     * Creates a general join party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a general join party.
     */
    public static GeneralJoinParty createParty(Abb3Party abb3Party, GeneralJoinConfig config) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (config.getGeneralJoinPtoType()) {
            case GENERAL_JOIN_HZF22:
                return new Hzf22GeneralJoinParty(abb3Party, (Hzf22GeneralJoinConfig) config);
            default:
                throw new IllegalArgumentException("Invalid config.getGeneralJoinPtoType() in creating GeneralJoinParty");
        }
    }

    public static GeneralJoinConfig createDefaultConfig(SecurityModel securityModel) {
        return new Hzf22GeneralJoinConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}
