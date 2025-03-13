package edu.alibaba.mpc4j.work.scape.s3pc.db.group;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * @author Feng Han
 * @date 2025/2/28
 */
public interface GroupConfig extends MultiPartyPtoConfig {
    /**
     * get the comparator type
     */
    ComparatorType getComparatorTypes();
}
