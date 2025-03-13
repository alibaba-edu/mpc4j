package edu.alibaba.mpc4j.work.scape.s3pc.opf.merge;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.MergeFactory.MergeType;

/**
 * interface of merge protocol configuration.
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public interface MergeConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    MergeType getMergeType();
}
