package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.ThreePartyOpfPto;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortFnParam;

/**
 * Interface for three-party sorting
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public interface PgSortParty extends ThreePartyOpfPto {
    /**
     * set up the usage of this function
     *
     * @param params the parameters indicating the function and parameters used on one invocation
     */
    long[] setUsage(PgSortFnParam... params);

    /**
     * get the permutation representing the sorting of input
     * it can be considered that the input are concatenate in order and then be sorted
     * for example, the concatenated input is {3, 6, 1, 2}, after the execution, the output is {2, 3, 0, 1}.
     *
     * @param input   the input array
     * @param bitLens the valid number of bits of each input
     */
    TripletLongVector perGen4MultiDim(TripletLongVector[] input, int[] bitLens) throws MpcAbortException;

    /**
     * get the permutation representing the sorting of input
     * it can be considered that the input are concatenate in order and then be sorted
     * for example, the concatenated input is {3, 6, 1, 2}, after the execution, the output is {2, 3, 0, 1}.
     *
     * @param input       the input array
     * @param bitLens     the valid number of bits of each input
     * @param saveSortRes the parameter to save the sorted result of input
     */
    TripletLongVector perGen4MultiDimWithOrigin(TripletLongVector[] input, int[] bitLens, TripletZ2Vector[] saveSortRes) throws MpcAbortException;

    /**
     * get the permutation representing the sorting of input
     * for example, the input is {3, 6, 1, 2}, after the execution, the output is {2, 3, 0, 1}.
     *
     * @param input the input array
     */
    TripletZ2Vector[] perGen(TripletZ2Vector[] input) throws MpcAbortException;

    /**
     * get the permutation representing the sorting of input, and sort the original data
     * for example, the input is {3, 6, 1, 2}, after the execution:
     * the output is {2, 3, 0, 1}, and the input is the sorted data {1, 2, 3, 6}
     *
     * @param input the input array
     */
    TripletZ2Vector[] perGenAndSortOrigin(TripletZ2Vector[] input) throws MpcAbortException;
}

