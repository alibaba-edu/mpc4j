package edu.alibaba.mpc4j.work.scape.s3pc.opf.agg;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggFactory.AggPtoType;

/**
 * @author Feng Han
 * @date 2025/2/26
 */
public interface AggConfig extends MultiPartyPtoConfig {
    /**
     * get the protocol type
     *
     * @return the protocol type
     */
    AggPtoType getPtoType();
    /**
     * get comparator type
     *
     * @return comparator type
     */
    ComparatorType getComparatorTypes();
}
