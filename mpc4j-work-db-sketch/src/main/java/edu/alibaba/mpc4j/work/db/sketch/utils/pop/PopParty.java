package edu.alibaba.mpc4j.work.db.sketch.utils.pop;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.ThreePartyOpfPto;

/**
 * Interface for Pop (Permute-and-Open) protocol.
 * Pop applies a random permutation to secret-shared data and then reveals (opens) the permuted data.
 * This is used to securely reveal data after permutation in the S³ framework.
 */
public interface PopParty extends ThreePartyOpfPto {
    /**
     * Set the resource usage of the protocol
     *
     * @param params the input parameters specifying the operation details
     * @return array of required tuple numbers [z2Tuples, z64Tuples]
     */
    long[] setUsage(PopFnParam... params);

    /**
     * Pop operation with index-based selection
     * Removes the element at the specified index and returns the remaining values
     *
     * @param input Input table of secret-shared values
     * @param index Target index indicating which element should be popped
     * @return array of remaining secret-shared values after popping
     * @throws MpcAbortException if the protocol execution fails
     */
    TripletZ2Vector[] pop(TripletZ2Vector[] input, TripletZ2Vector[] index) throws MpcAbortException;

    /**
     * Pop operation with flag-based selection
     * Removes elements indicated by the flag and returns the remaining values
     *
     * @param input Input table of secret-shared values
     * @param flag  Indicator flag, where each bit indicates whether the corresponding element should be popped
     * @return array of remaining secret-shared values after popping
     */
    TripletZ2Vector[] pop(TripletZ2Vector[] input, TripletZ2Vector flag);
}
