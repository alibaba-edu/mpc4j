package edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general.GeneralSemiJoinFactory.GeneralSemiJoinPtoType;

/**
 * config for general semi-join
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public interface GeneralSemiJoinConfig extends MultiPartyPtoConfig {
    /**
     * Gets the general join pto type.
     *
     * @return the general join pto type.
     */
    GeneralSemiJoinPtoType getGeneralSemiJoinPtoType();
}
