package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory.PgSortType;

/**
 * Interface for three-party sorting configure
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public interface PgSortConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    PgSortType getSortType();
    /**
     * whether the sorting protocol is stable
     */
    boolean isStable();
    /**
     * set the comparator type in the sorting algorithm
     */
    void setComparatorType(ComparatorType comparatorType);
}
