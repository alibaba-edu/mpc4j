package edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.ThreePartyDbPto;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.SemiJoinFnParam;

/**
 * PkPk semi-join party interface
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public interface PkPkSemiJoinParty extends ThreePartyDbPto {
    /**
     * compute the required tuple number for all operations
     *
     * @param params input information of the PkPk join protocol
     * @return [bitTupleNum, longTupleNum]
     */
    long[] setUsage(SemiJoinFnParam... params);

    /**
     * compute the indicator flag indicating whether a key in the right table exists in the left table
     *
     * @param x         input left table
     * @param y         input right table
     * @param xKeyIndex the indexes of join key of the left table
     * @param yKeyIndex the indexes of join key of the right table
     * @param withDummy whether there exist dummy rows in input
     * @param inputIsSorted whether the input data are already sorted based on their join keys
     * @return an indicator flag vector: for each row in the right table, indicating whether its key exists in the left table
     */
    TripletZ2Vector semiJoin(TripletZ2Vector[] x, TripletZ2Vector[] y,
                             int[] xKeyIndex, int[] yKeyIndex, boolean withDummy, boolean inputIsSorted) throws MpcAbortException;
}
