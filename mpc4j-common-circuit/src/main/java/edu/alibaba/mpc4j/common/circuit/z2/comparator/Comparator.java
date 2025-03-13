package edu.alibaba.mpc4j.common.circuit.z2.comparator;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * interface of Comparator.
 *
 * @author Feng Han
 * @date 2025/2/27
 */
public interface Comparator {
    /**
     * x ≤ y. compare for data without sign bit, which means the values of data in [0, 2^l - 1];
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, where z = (x ≤ y).
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector leq(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException;
}
