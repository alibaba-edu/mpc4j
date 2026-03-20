package edu.alibaba.mpc4j.work.db.sketch.utils.orderselect;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.ThreePartyOpfPto;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.OrderSelectOperations.OrderSelectFnParam;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Interface for three-party oblivious order select protocol.
 * Used for selecting elements from secret-shared data based on their sorted order,
 * particularly useful for quantile queries in GK sketch.
 */
public interface OrderSelectParty extends ThreePartyOpfPto {
    /**
     * Set the resource usage of the protocol
     *
     * @param params the parameters indicating the function and parameters used on one invocation
     * @return array of required tuple numbers [z2Tuples, z64Tuples]
     */
    long[] setUsage(OrderSelectFnParam... params);

    /**
     * Get the permutation representing the sorting of input and select elements in the specified range
     * The input is considered as concatenated and then sorted. The output in the range maintains sorted order.
     * For example, concatenated input is {3, 6, 1, 2}, range is {1,3}, after execution: Pair({_, 3, 0, _}, {2,3})
     * The second output contains the elements in the required range in sorted order.
     *
     * @param input   the input array of secret-shared long values
     * @param bitLens the valid bit length for each input vector
     * @param range   the required range, {from, to} means values with positions in [from, to) after sorting
     * @return Pair containing <the permutation, the elements in the required range>
     * @throws MpcAbortException if the protocol execution fails
     */
    Pair<TripletZ2Vector[], TripletLongVector[]> orderSelect(TripletLongVector[] input, int[] bitLens, int[] range) throws MpcAbortException;

    /**
     * Get the permutation for selecting elements in the specified range without maintaining sorted order
     * The input is considered as concatenated and then sorted. The output in the range is not sorted.
     * For example, concatenated input is {3, 6, 1, 2}, range is {1,3}, output may be Pair({_, 3, 0, _}, {2,3}) or Pair({_, 0, 3, _}, {3,2})
     * The second output contains the elements in the required range (unsorted).
     *
     * @param input   the input array of secret-shared long values
     * @param bitLens the valid bit length for each input vector
     * @param range   the required range, {from, to} means values with positions in [from, to) after sorting
     * @return Pair containing <the permutation, the elements in the required range>
     * @throws MpcAbortException if the protocol execution fails
     */
    Pair<TripletZ2Vector[], TripletLongVector[]> selectRangeNoOrder(TripletLongVector[] input, int[] bitLens, int[] range) throws MpcAbortException;

    /**
     * Get the permutation such that the required data can be permuted into the corresponding position
     * For example, input is {3, 6, 1, 2}, range is {1,3}, after execution: Pair({_, 3, 0, _}, {2,3})
     * The _ means values can be any number as long as the first output is a valid permutation
     * The second output contains the elements in the required range in sorted order.
     *
     * @param input the input array of secret-shared binary values
     * @param range the required range, {from, to} means values with positions in [from, to) after sorting
     * @return Pair containing <the permutation, the elements in the required range>
     * @throws MpcAbortException if the protocol execution fails
     */
    Pair<TripletZ2Vector[], TripletZ2Vector[]> orderSelect(TripletZ2Vector[] input, int[] range) throws MpcAbortException;

    /**
     * Get the permutation for selecting elements in the specified range without maintaining sorted order
     * For example, input is {3, 6, 1, 2}, range is {1,3}, after execution: Pair({_, 3, 0, _}, {2,3})
     * The _ means values can be any number as long as the first output is a valid permutation
     * The second output contains the elements in the required range (unsorted).
     *
     * @param input the input array of secret-shared binary values
     * @param range the required range, {from, to} means values with positions in [from, to) after sorting
     * @return Pair containing <the permutation, the elements in the required range>
     * @throws MpcAbortException if the protocol execution fails
     */
    Pair<TripletZ2Vector[], TripletZ2Vector[]> selectRangeNoOrder(TripletZ2Vector[] input, int[] range) throws MpcAbortException;
}
