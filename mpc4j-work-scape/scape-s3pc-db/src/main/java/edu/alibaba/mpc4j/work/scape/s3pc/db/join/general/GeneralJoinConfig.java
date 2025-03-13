package edu.alibaba.mpc4j.work.scape.s3pc.db.join.general;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.general.GeneralJoinFactory.GeneralJoinPtoType;

/**
 * config of the general join protocol
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public interface GeneralJoinConfig extends MultiPartyPtoConfig {
    /**
     * get the type of protocol
     *
     * @return the type of this protocol
     */
    GeneralJoinPtoType getGeneralJoinPtoType();
}
