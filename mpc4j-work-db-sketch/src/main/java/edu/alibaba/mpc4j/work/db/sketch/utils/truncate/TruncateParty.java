package edu.alibaba.mpc4j.work.db.sketch.utils.truncate;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.ThreePartyDbPto;

/**
 * Interface for Truncate protocol.
 * Truncate operation is used to truncate secret-shared bit vectors, typically after the compact step in the Merge protocol
 * to keep only the first s elements.
 */
public interface TruncateParty extends ThreePartyDbPto {
    /**
     * Set the resource usage of the protocol
     *
     * @param params the input parameters specifying the operation details
     * @return array of required tuple numbers [z2Tuples, z64Tuples]
     */
    long[] setUsage(TruncateFnParam... params);

    /**
     * Compute group sum and truncate all valid values to the front of the array
     * This operation groups data by the group flag, computes sums within each group, and truncates to keep only valid results
     *
     * @param payload      Input group payload values to be summed
     * @param groupFlag    Group flag, e.g., [0,1,1,...1,0,...], where 0 represents the first element in each group
     * @param truncateSize Target truncate size, must be large enough to save all valid rows
     * @param keys         Optional group key (may be null if no grouping needed)
     * @return array containing [group_key(may be empty if input keys is null), group_agg_result, valid_flag]
     * @throws MpcAbortException if the protocol execution fails
     */
    TripletLongVector[] groupSumAndTruncate(TripletLongVector[] payload, TripletLongVector groupFlag, int truncateSize, TripletLongVector... keys) throws MpcAbortException;
}