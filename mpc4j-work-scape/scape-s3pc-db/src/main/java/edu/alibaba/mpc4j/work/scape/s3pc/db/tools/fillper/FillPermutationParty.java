package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.ThreePartyDbPto;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPerOperations.FillPerFnParam;

/**
 * permutation completion party
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public interface FillPermutationParty extends ThreePartyDbPto {

    /**
     * set up the usage of functions, and update the tuple info
     *
     * @param params the usage of this function
     */
    long[] setUsage(FillPerFnParam... params);

    /**
     * complete an injective function into a permutation by adding the index that has not appeared before
     * the input is required to have order, such that
     * (1) the data with EqFlag = 0 or 1 is sorted in ascending order in their group
     * (2) all data with EqFlag = 0 are smaller than all data with EqFlag = 1
     *
     * @param leftIndex / rightIndex: an injective function [0, index.len) -> [0, m)
     * @param leftEqual / rightIndex: the equal sign of a row
     * @param m         the size of the target permutation
     */
    TripletLongVector[] twoPermutationCompletion(
        TripletLongVector leftIndex, TripletLongVector leftEqual,
        TripletLongVector rightIndex, TripletLongVector rightEqual, int m) throws MpcAbortException;

    /**
     * complete an injective function into a permutation by adding the index that has not appeared before
     *
     * @param index an injective function [0, index.len) -> [0, m)
     * @param equalSign the equal sign of a row
     * @param m the size of the target permutation
     */
    TripletLongVector permutationCompletion(
        TripletLongVector index, TripletLongVector equalSign, int m) throws MpcAbortException;

}
