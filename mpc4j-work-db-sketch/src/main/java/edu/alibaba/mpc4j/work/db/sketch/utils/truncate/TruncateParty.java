package edu.alibaba.mpc4j.work.db.sketch.utils.truncate;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.ThreePartyDbPto;

public interface TruncateParty extends ThreePartyDbPto {
    /**
     * set the usage of the protocol
     *
     * @param params the input parameters
     * @return the required tuple number
     */
    long[] setUsage(TruncateFnParam... params);

    /**
     * obtain group sum result and truncate all valid values into the front
     *
     * @param payload      Input group payload
     * @param groupFlag    group flag, sucha as [0,1,1,...1,0,...], where 0 represents the first element in each group
     * @param truncateSize target truncate size, to make sure that all valid rows are saved, this value should be large enough
     * @param keys         group key: may be null
     * @return [group_key(maybe nothing in result if input keys is null), group_agg_result, valid_flag]
     */
    TripletLongVector[] groupSumAndTruncate(TripletLongVector[] payload, TripletLongVector groupFlag, int truncateSize, TripletLongVector... keys) throws MpcAbortException;
}