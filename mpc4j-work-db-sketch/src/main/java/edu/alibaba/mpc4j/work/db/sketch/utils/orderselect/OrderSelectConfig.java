package edu.alibaba.mpc4j.work.db.sketch.utils.orderselect;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.OrderSelectFactory.OrderSelectType;

/**
 * Configuration interface for three-party order select protocol.
 * Defines the protocol type, stability, and comparator settings.
 */
public interface OrderSelectConfig extends MultiPartyPtoConfig {
    /**
     * Get the protocol type
     *
     * @return protocol type (e.g., QUICK_ORDER_SELECT)
     */
    OrderSelectType getOrderSelectType();

    /**
     * Check if the range select protocol is stable (maintains relative order of equal elements)
     *
     * @return true if stable, false otherwise
     */
    boolean isStable();

    /**
     * Set the comparator type in the sorting algorithm
     *
     * @param comparatorType the comparator type to use
     */
    void setComparatorType(ComparatorType comparatorType);
}
