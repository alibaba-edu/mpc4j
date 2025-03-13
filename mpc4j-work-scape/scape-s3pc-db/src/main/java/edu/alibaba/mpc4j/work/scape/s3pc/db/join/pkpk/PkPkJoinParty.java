package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.ThreePartyDbPto;

/**
 * Interface for the PkPk join protocol.
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public interface PkPkJoinParty extends ThreePartyDbPto {
    /**
     * compute the required tuple number for all operations
     *
     * @param params input information of the PkPk join protocol
     * @return [bitTupleNum, longTupleNum]
     */
    long[] setUsage(PkPkJoinFnParam... params);

    /**
     * compute the inner join for pk-pk join
     *
     * @param left          left table
     *                      the last vector in left table is the valid indicator flag, indicating whether the element is valid
     * @param right         right table
     * @param leftKeyIndex  the indexes of key in left table
     *                      for example: leftKeyIndex = {0, 1, 2} means the first three vectors in left table are the join key
     * @param rightKeyIndex the indexes of key in right table
     * @param withDummy     the input data has dummy elements
     * @param inputIsSorted whether the input data are already sorted based on their join keys
     * @return the result of inner join, the length is the same as the right table
     * @throws MpcAbortException the protocol failure abort exception
     */
    TripletZ2Vector[] primaryKeyInnerJoin(TripletZ2Vector[] left, TripletZ2Vector[] right, int[] leftKeyIndex,
                                          int[] rightKeyIndex, boolean withDummy, boolean inputIsSorted) throws MpcAbortException;

}
