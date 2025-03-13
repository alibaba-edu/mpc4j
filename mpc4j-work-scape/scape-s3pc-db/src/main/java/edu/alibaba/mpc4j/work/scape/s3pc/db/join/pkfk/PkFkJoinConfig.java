package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk.PkFkJoinFactory.PkFkJoinPtoType;

/**
 * interface of PkFk Join Config
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public interface PkFkJoinConfig extends MultiPartyPtoConfig {
    /**
     * Gets the PkFkJoinPtoType.
     *
     * @return the PkFkJoinPtoType.
     */
    PkFkJoinPtoType getPkFkJoinPtoType();
}
