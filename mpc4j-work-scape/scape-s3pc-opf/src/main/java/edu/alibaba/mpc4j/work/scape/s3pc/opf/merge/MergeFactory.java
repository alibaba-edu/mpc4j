package edu.alibaba.mpc4j.work.scape.s3pc.opf.merge;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.bitonic.BitonicMergeConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.bitonic.BitonicMergeParty;

/**
 * factory of merge protocols
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public class MergeFactory implements PtoFactory {
    /**
     * the protocol type
     */
    public enum MergeType {
        /**
         * bitonic merge
         */
        MERGE_BITONIC,
    }

    /**
     * Creates a permutation party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a z2c party.
     */
    public static MergeParty createParty(Abb3Party abb3Party, MergeConfig config) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (config.getMergeType()) {
            case MERGE_BITONIC:
                return new BitonicMergeParty(abb3Party, (BitonicMergeConfig) config);
            default:
                throw new IllegalArgumentException("Invalid config.getMergeType() in creating MergeParty");
        }
    }

    public static MergeConfig createDefaultConfig(SecurityModel securityModel) {
        return new BitonicMergeConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}
