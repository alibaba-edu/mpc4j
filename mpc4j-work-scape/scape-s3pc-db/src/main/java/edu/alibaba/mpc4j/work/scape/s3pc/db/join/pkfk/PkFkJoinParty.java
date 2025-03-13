package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.ThreePartyDbPto;

/**
 * interface of PkFk Join Party
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public interface PkFkJoinParty extends ThreePartyDbPto {
    /**
     * compute the required tuple number for all operations
     *
     * @param params input information of the PkFk join protocol
     * @return [bitTupleNum, longTupleNum]
     */
    long[] setUsage(PkFkJoinFnParam... params);

    /**
     * 输入两个Arithmetic sharing的数据库，然后计算出innerJoin的结果
     *
     * @param uTable     the table with unique key
     * @param nuTable    the table with non-unique key
     * @param uKeyIndex  the indexes of the join keys of uTable
     * @param nuKeyIndex the indexes of the join keys of nuTable
     * @param inputIsSorted the input is sorted in the order of join_key and valid_flag
     * @return [(key, [leftPayload], [rightPayload], F)]
     */
    TripletLongVector[] innerJoin(TripletLongVector[] uTable, TripletLongVector[] nuTable,
                                  int[] uKeyIndex, int[] nuKeyIndex, boolean inputIsSorted) throws MpcAbortException;

}
