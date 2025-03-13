package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.PkPkJoinFactory.PkPkJoinPtoType;

/**
 * config of the PkPk join protocol
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public interface PkPkJoinConfig extends MultiPartyPtoConfig {
    /**
     * get the join protocol type
     */
    PkPkJoinPtoType getPkPkJoinPtoType();
}
