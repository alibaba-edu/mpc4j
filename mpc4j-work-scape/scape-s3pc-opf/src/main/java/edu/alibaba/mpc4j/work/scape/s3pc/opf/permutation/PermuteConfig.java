package edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory.PermuteType;

/**
 * Interface for three-party permutation configure
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public interface PermuteConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    PermuteType getPermuteType();
}
