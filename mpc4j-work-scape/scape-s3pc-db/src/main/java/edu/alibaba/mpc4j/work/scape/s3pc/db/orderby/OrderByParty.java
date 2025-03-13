package edu.alibaba.mpc4j.work.scape.s3pc.db.orderby;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.ThreePartyDbPto;

/**
 * interface of order-by
 *
 * @author Feng Han
 * @date 2025/3/4
 */
public interface OrderByParty extends ThreePartyDbPto {
    /**
     * compute the required tuple number for all operations
     *
     * @param params input information of the order-by protocol
     * @return [bitTupleNum, longTupleNum]
     */
    long[] setUsage(OrderByFnParam... params);

    /**
     * compute the indicator flag indicating whether a key in the right table exists in the left table
     *
     * @param table    input table
     * @param keyIndex the indexes of order-by attributes of the table
     * @return a sorted table
     */
    void orderBy(TripletZ2Vector[] table, int[] keyIndex) throws MpcAbortException;

    /**
     * compute the indicator flag indicating whether a key in the right table exists in the left table
     *
     * @param table    input table
     * @param keyIndex the indexes of order-by attributes of the table
     * @return a sorted table
     */
    void orderBy(TripletLongVector[] table, int[] keyIndex) throws MpcAbortException;
}
