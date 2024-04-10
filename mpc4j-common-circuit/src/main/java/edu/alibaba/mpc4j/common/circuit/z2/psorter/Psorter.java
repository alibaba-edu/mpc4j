package edu.alibaba.mpc4j.common.circuit.z2.psorter;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Sorter interface.
 *
 * @author Feng Han
 * @date 2023/10/26
 */
public interface Psorter {
    /**
     * get the estimated number of AND gate
     *
     * @param dataNum input data number
     * @param dataDim data dimension
     */
    long getAndGateNum(int dataNum, int dataDim);

    /**
     * get the permutation representing the sort of xiArrays in ascending order.
     *
     * @param xiArrays        xi arrays, in column form.
     *                        for example: c0 with 5 bits and c1 with 7 bits, we want to sort data in order of c0|c1
     *                        then input: xiArrays.length = 2, xiArrays[0].length = 5, xiArrays[1].length = 7
     * @param needPermutation whether the permutation is needed or not
     * @param needStable      whether stable sorting or not
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector[] sort(MpcZ2Vector[][] xiArrays, boolean needPermutation, boolean needStable) throws MpcAbortException;

    /**
     * Sorts in the specified order.
     * for example: c0 with 5 bits and c1 with 7 bits, we want to sort data in order of c0↓ | c1↑
     * then input: xiArrays.length = 2, xiArrays[0].length = 5, xiArrays[1].length = 7
     * and dir[0] = true, dir[1] = false
     *
     * @param xiArrays        xi arrays, in column form.
     * @param dir             sorting order, ture for ascending..
     * @param needPermutation whether the permutation is needed or not
     * @param needStable      whether stable sorting or not
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector[] sort(MpcZ2Vector[][] xiArrays, PlainZ2Vector dir, boolean needPermutation, boolean needStable) throws MpcAbortException;

    /**
     * Sorts in the specified order.
     * for example: c0 with 5 bits and c1 with 7 bits, we want to sort data in order of c0↓ | c1↑
     * then input: xiArrays.length = 2, xiArrays[0].length = 5, xiArrays[1].length = 7
     * and dir[0] = true, dir[1] = false
     *
     * @param xiArrays        xi arrays, in column form.
     * @param payloadArrays   payloads needed to be sorted based on the order of xiArrays
     *                        after invoking this function, the values will be refreshed
     * @param dir             sorting order, ture for ascending..
     * @param needPermutation whether the permutation is needed or not
     * @param needStable      whether stable sorting or not
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector[] sort(MpcZ2Vector[][] xiArrays, MpcZ2Vector[][] payloadArrays, PlainZ2Vector dir, boolean needPermutation, boolean needStable) throws MpcAbortException;
}
