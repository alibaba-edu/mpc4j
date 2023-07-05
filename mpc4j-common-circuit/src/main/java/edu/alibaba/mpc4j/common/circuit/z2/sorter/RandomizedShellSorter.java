package edu.alibaba.mpc4j.common.circuit.z2.sorter;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Randomized Shell Sorter. Randomized Shell sort has a complexity of O(m log m) comparisons, and is data-oblivious
 * since its control flow is independent of the input.
 * <p>
 * The scheme comes from the following paper:
 *
 * <p>
 * Goodrich, Michael T. "Randomized shellsort: A simple oblivious sorting algorithm." Proceedings of the twenty-first
 * annual ACM-SIAM symposium on Discrete Algorithms. Society for Industrial and Applied Mathematics, 2010.
 * </p>
 *
 * @author Li Peng
 * @date 2023/6/27
 */
public class RandomizedShellSorter extends AbstractSortingNetwork {
    /**
     * number of region compare-exchange repetitions
     */
    public static final int C = 4;

    public RandomizedShellSorter(Z2IntegerCircuit circuit) {
        super(circuit);
    }

    @Override
    public void sort(MpcZ2Vector[][] xiArrays, MpcZ2Vector dir) throws MpcAbortException {
        randomizedShellSort(xiArrays);
    }

    private void permuteRandom(int[] indexes, SecureRandom rand) {
        for (int i = 0; i < indexes.length; i++) {
            // Use the Knuth random permutation algorithm
            exchange(indexes, i, rand.nextInt(indexes.length - i) + i);
        }
    }

    /**
     * compare-exchange two regions of length offset each
     */
    private void compareRegions(MpcZ2Vector[][] xiArray, int s, int t, int offset, SecureRandom rand) throws MpcAbortException {
        // do C region compare-exchanges
        for (int count = 0; count < C; count++) {
            // index offset array
            int[] mate = IntStream.range(0, offset).toArray();
            // comment this out to get xiArray deterministic Shellsort
            permuteRandom(mate, rand);
            for (int i = 0; i < offset; i++) {
                // exchange region1 with random locations of region2
                exchangeWhenDescending(xiArray, s + i, t + mate[i]);
            }
        }
    }

    public void randomizedShellSort(MpcZ2Vector[][] xiArray) throws MpcAbortException {
        int n = xiArray.length;
        SecureRandom rand = new SecureRandom();
        for (int offset = n / 2; offset > 0; offset /= 2) {
            for (int i = 0; i < n - offset; i += offset) {
                // compare-exchange up
                compareRegions(xiArray, i, i + offset, offset, rand);
            }
            for (int i = n - offset; i >= offset; i -= offset) {
                // compare-exchange down
                compareRegions(xiArray, i - offset, i, offset, rand);
            }
            for (int i = 0; i < n - 3 * offset; i += offset) {
                // compare 3 hops up
                compareRegions(xiArray, i, i + 3 * offset, offset, rand);
            }
            for (int i = 0; i < n - 2 * offset; i += offset) {
                // compare 2 hops up
                compareRegions(xiArray, i, i + 2 * offset, offset, rand);
            }
            for (int i = 0; i < n; i += 2 * offset) {
                // compare odd-even regions
                compareRegions(xiArray, i, i + offset, offset, rand);
            }
            for (int i = offset; i < n - offset; i += 2 * offset) {
                // compare even-odd regions
                compareRegions(xiArray, i, i + offset, offset, rand);
            }
        }
    }

    /**
     * Compare and exchange two items, exchange when item of low index is larger than item of high index.
     *
     * @param xiArray xiArray.
     * @param i       i.
     * @param j       j.
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected void exchangeWhenDescending(MpcZ2Vector[][] xiArray, int i, int j) throws MpcAbortException {
        if (i >= xiArray.length || j >= xiArray.length) {
            return;
        }
        compareExchange(xiArray, i, j, party.createOnes(num));
    }

    /**
     * exchange two items.
     *
     * @param indexes indexes.
     * @param i       i.
     * @param j       j.
     */
    public static void exchange(int[] indexes, int i, int j) {
        int temp = indexes[i];
        indexes[i] = indexes[j];
        indexes[j] = temp;
    }
}


