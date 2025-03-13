package edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.PkPkSemiJoinFactory.PkPkSemiJoinPtoType;

/**
 * config of the PkPk semi-join protocol
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public interface PkPkSemiJoinConfig extends MultiPartyPtoConfig {
    /**
     * get the type of the semi-join protocol
     */
    PkPkSemiJoinPtoType getPkPkSemiJoinPtoType();
}
