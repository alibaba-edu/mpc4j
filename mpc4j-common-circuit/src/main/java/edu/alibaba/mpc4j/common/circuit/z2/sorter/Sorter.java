package edu.alibaba.mpc4j.common.circuit.z2.sorter;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Sorter interface.
 *
 * @author Li Peng
 * @date 2023/6/12
 */
public interface Sorter {
    /**
     * Sorts in ascending order.
     *
     * @param xiArrays xi arrays.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void sort(MpcZ2Vector[][] xiArrays) throws MpcAbortException;

    /**
     * Sorts in the specified order.
     *
     * @param xiArrays xi arrays.
     * @param dir      sorting order, ture for ascending..
     * @throws MpcAbortException the protocol failure aborts.
     */
    void sort(MpcZ2Vector[][] xiArrays, MpcZ2Vector dir)
        throws MpcAbortException;
}
