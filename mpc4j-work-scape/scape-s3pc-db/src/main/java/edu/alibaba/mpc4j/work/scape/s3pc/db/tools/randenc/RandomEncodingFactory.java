package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.mrr20.Mrr20RandomEncodingConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.mrr20.Mrr20RandomEncodingParty;

/**
 * factory for random encoding.
 *
 * @author Feng Han
 * @date 2025/2/25
 */
public class RandomEncodingFactory {
    /**
     * sortSign type
     */
    public enum EncodingPtoType {
        /**
         * MRR20
         */
        MRR20,
    }

    /**
     * Creates a general join party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a general join party.
     */
    public static RandomEncodingParty createParty(Abb3Party abb3Party, RandomEncodingConfig config) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (config.getPtoType()) {
            case MRR20:
                return new Mrr20RandomEncodingParty(abb3Party, (Mrr20RandomEncodingConfig) config);
            default:
                throw new IllegalArgumentException("Invalid config.getPtoType() in creating RandomEncodingParty");
        }
    }

    public static RandomEncodingConfig createDefaultConfig(SecurityModel securityModel) {
        return new Mrr20RandomEncodingConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }

}
