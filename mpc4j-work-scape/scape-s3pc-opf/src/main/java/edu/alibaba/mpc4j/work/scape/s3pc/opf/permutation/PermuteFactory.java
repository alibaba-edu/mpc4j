package edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.ahi22.Ahi22PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.ahi22.Ahi22PermuteParty;

/**
 * 3pc oblivious permutation party factory.
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class PermuteFactory implements PtoFactory {
    /**
     * the protocol type
     */
    public enum PermuteType {
        /**
         * permutation protocols in Ahi22
         */
        PERMUTE_AHI22,
    }

    /**
     * Creates a permutation party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a z2c party.
     */
    public static PermuteParty createParty(Abb3Party abb3Party, PermuteConfig config) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (config.getPermuteType()) {
            case PERMUTE_AHI22:
                return new Ahi22PermuteParty(abb3Party, (Ahi22PermuteConfig) config);
            default:
                throw new IllegalArgumentException("Invalid config.getPermuteType() in creating PermuteParty");
        }
    }

    public static PermuteConfig createDefaultConfig(SecurityModel securityModel) {
        return new Ahi22PermuteConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}
