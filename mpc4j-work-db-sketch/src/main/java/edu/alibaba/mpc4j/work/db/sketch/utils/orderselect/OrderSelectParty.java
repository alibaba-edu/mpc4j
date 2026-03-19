package edu.alibaba.mpc4j.work.db.sketch.utils.orderselect;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.ThreePartyOpfPto;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.OrderSelectOperations.OrderSelectFnParam;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Interface for three-party order select
 */
public interface OrderSelectParty extends ThreePartyOpfPto {
    /**
     * set up the usage of this function
     *
     * @param params the parameters indicating the function and parameters used on one invocation
     */
    long[] setUsage(OrderSelectFnParam... params);

    /**
     * get the permutation representing the sorting of input
     * it can be considered that the input are concatenate in order and then be sorted
     * bitLens means the valid bit length for each input vector
     * for example, the concatenated input is {3, 6, 1, 2}, range is {1,3}, after the execution, the output is Pair({_, 3, 0, _}, {2,3})
     * the second output is the elements in the required range.
     *
     * @param input   the input array
     * @param bitLens the valid number of bits of each input
     * @param range   the required range, {from, to} means we want the values whose positions is in range [from, to) after sorting
     * @return <TripletZ2Vector[], TripletLongVector[]>: <the permutation, the elements in the required range>
     */
    Pair<TripletZ2Vector[], TripletLongVector[]> orderSelect(TripletLongVector[] input, int[] bitLens, int[] range) throws MpcAbortException;

    /**
     * get the permutation representing the selection of input should be sorted to the specific range, but the output in the range is not sorted
     * it can be considered that the input are concatenate in order and then be sorted
     * bitLens means the valid bit length for each input vector
     * for example, the concatenated input is {3, 6, 1, 2}, range is {1,3}, after the execution, the output may be Pair({_, 3, 0, _}, {2,3}) or Pair({_, 0, 3, _}, {3,2})
     * the second output is the elements in the required range.
     *
     * @param input   the input array
     * @param bitLens the valid number of bits of each input
     * @param range   the required range, {from, to} means we want the values whose positions is in range [from, to) after sorting
     * @return <TripletZ2Vector[], TripletLongVector[]>: <the permutation, the elements in the required range>
     */
    Pair<TripletZ2Vector[], TripletLongVector[]> selectRangeNoOrder(TripletLongVector[] input, int[] bitLens, int[] range) throws MpcAbortException;

    /**
     * get the permutation such that the required data can be permuted into the corresponding position
     * for example, the input is {3, 6, 1, 2}, range is {1,3}, after the execution, the output is Pair({_, 3, 0, _}, {2,3})
     * the _ in the above example means that the values can possibly be any number as long as the first output is a permutation
     * the second output is the elements in the required range.
     *
     * @param input the input array
     * @param range the required range, {from, to} means we want the values whose positions is in range [from, to) after sorting
     * @return <TripletZ2Vector[], TripletZ2Vector[]>: <the permutation, the elements in the required range>
     */
    Pair<TripletZ2Vector[], TripletZ2Vector[]> orderSelect(TripletZ2Vector[] input, int[] range) throws MpcAbortException;

    /**
     * get the permutation representing the selection of input should be sorted to the specific range, but the output in the range is not sorted
     * for example, the input is {3, 6, 1, 2}, range is {1,3}, after the execution, the output is Pair({_, 3, 0, _}, {2,3})
     * the _ in the above example means that the values can possibly be any number as long as the first output is a permutation
     * the second output is the elements in the required range.
     *
     * @param input the input array
     * @param range the required range, {from, to} means we want the values whose positions is in range [from, to) after sorting
     * @return <TripletZ2Vector[], TripletZ2Vector[]>: <the permutation, the elements in the required range>
     */
    Pair<TripletZ2Vector[], TripletZ2Vector[]> selectRangeNoOrder(TripletZ2Vector[] input, int[] range) throws MpcAbortException;
}
