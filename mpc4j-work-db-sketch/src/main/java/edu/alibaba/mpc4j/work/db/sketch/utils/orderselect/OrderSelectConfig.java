package edu.alibaba.mpc4j.work.db.sketch.utils.orderselect;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.OrderSelectFactory.OrderSelectType;

/**
 * Interface for three-party order select configure
 */
public interface OrderSelectConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    OrderSelectType getOrderSelectType();

    /**
     * whether the range select protocol is stable
     */
    boolean isStable();

    /**
     * set the comparator type in the sorting algorithm
     */
    void setComparatorType(ComparatorType comparatorType);
}
