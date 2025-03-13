package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.hzf22.Hzf22FillPermutationConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.hzf22.Hzf22FillPermutationParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.kks20.Kks20FillPermutationConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.kks20.Kks20FillPermutationParty;

/**
 * filling permutation factory
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public class FillPermutationFactory implements PtoFactory {
    /**
     * filling permutation type
     */
    public enum FillType{
        /**
         * SOPRP-based
         */
        SOPRP_BASED_HZF22,
        /**
         * butterfly net-based
         */
        BUTTERFLY_NET_KKS20,
    }

    /**
     * Creates a permutation generation sorting party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a permutation generation sorting party.
     */
    public static FillPermutationParty createParty(Abb3Party abb3Party, FillPermutationConfig config) {
        switch (config.getFillType()) {
            case SOPRP_BASED_HZF22:
                return new Hzf22FillPermutationParty(abb3Party, (Hzf22FillPermutationConfig) config);
            case BUTTERFLY_NET_KKS20:
                return new Kks20FillPermutationParty(abb3Party, (Kks20FillPermutationConfig) config);
            default:
                throw new IllegalArgumentException("Invalid config.getFillType() in creating FillPermutationParty");
        }
    }

    public static FillPermutationConfig createDefaultConfig(SecurityModel securityModel) {
        return new Hzf22FillPermutationConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}
