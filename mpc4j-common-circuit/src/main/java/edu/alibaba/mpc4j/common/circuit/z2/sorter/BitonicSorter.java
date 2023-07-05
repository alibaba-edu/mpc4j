package edu.alibaba.mpc4j.common.circuit.z2.sorter;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.math.BigInteger;


/**
 * Bitonic Sorter. Bitonic sort has a complexity of O(m log^2 m) comparisons with small constant, and is data-oblivious
 * since its control flow is independent of the input.
 * <p>
 * The scheme comes from the following paper:
 *
 * <p>
 * Kenneth E. Batcher. 1968. Sorting Networks and Their Applications. In American Federation of Information Processing
 * Societies: AFIPS, Vol. 32. Thomson Book Company, Washington D.C., 307–314.
 * </p>
 *
 * @author Li Peng
 * @date 2023/6/12
 */
public class BitonicSorter extends AbstractSortingNetwork {

    public BitonicSorter(Z2IntegerCircuit circuit) {
        super(circuit);
    }

    @Override
    public void sort(MpcZ2Vector[][] xiArrays, MpcZ2Vector dir) throws MpcAbortException {
        bitonicSort(xiArrays, 0, xiArrays.length, dir);
    }

    private void bitonicSort(MpcZ2Vector[][] xiArrays, int start, int len, MpcZ2Vector dir) throws MpcAbortException {
        if (len > 1) {
            // Divide the array into two partitions and then sort
            // the partitions in different directions.
            int m = len / 2;
            bitonicSort(xiArrays, start, m, party.not(dir));
            bitonicSort(xiArrays, start + m, len - m, dir);
            // Merge the results.
            bitonicMerge(xiArrays, start, len, dir);
        }
    }

    /**
     * Sorts a bitonic sequence in the specified order.
     *
     * @param xiArray items.
     * @param start   start location
     * @param len     length.
     * @param dir     sorting order, ture for ascending.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void bitonicMerge(MpcZ2Vector[][] xiArray, int start, int len, MpcZ2Vector dir) throws MpcAbortException {
        if (len > 1) {
            // minimum power of two greater than or equal to len.
            int m = 1 << (BigInteger.valueOf(len - 1).bitLength() - 1);
            for (int i = start; i < start + len - m; i++) {
                compareExchange(xiArray, i, i + m, dir);
            }
            bitonicMerge(xiArray, start, m, dir);
            bitonicMerge(xiArray, start + m, len - m, dir);
        }
    }
}
