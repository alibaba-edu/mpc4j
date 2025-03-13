package edu.alibaba.mpc4j.work.scape.s3pc.db.orderby;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.OrderByFactory.OrderByPtoType;

/**
 * configure of order-by protocol
 *
 * @author Feng Han
 * @date 2025/3/4
 */
public interface OrderByConfig extends MultiPartyPtoConfig {
    /**
     * get the type of the semi-join protocol
     */
    OrderByPtoType getOrderByPtoType();
    /**
     * set the comparator type in the sorting algorithm
     */
    void setComparatorType(ComparatorType comparatorType);
}
