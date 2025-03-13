package edu.alibaba.mpc4j.work.scape.s3pc.db.join.general;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.ThreePartyDbPto;

/**
 * Interface for the general join protocol.
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public interface GeneralJoinParty extends ThreePartyDbPto {

    /**
     * compute the required tuple number for all operations
     *
     * @param params input information of the PkPk join protocol
     * @return [bitTupleNum, longTupleNum]
     */
    long[] setUsage(GeneralJoinFnParam... params);

    /**
     * 输入两个Arithmetic sharing的数据库，然后计算出innerJoin的结果
     *
     * @param left          输入的左表
     * @param right         输入的右表
     * @param leftKeyIndex  左表中key所在的index，例如left中的第一维是join_key，则leftKeyIndex为 new int[]{0}
     * @param rightKeyIndex 右表中key所在的index
     * @param m             upper bound of join result length
     * @param inputIsSorted the input is sorted in the order of join_key and valid_flag
     * @return [(key, [leftPayload], [rightPayload], F)]
     */
    TripletLongVector[] innerJoin(TripletLongVector[] left, TripletLongVector[] right,
                                  int[] leftKeyIndex, int[] rightKeyIndex, int m, boolean inputIsSorted) throws MpcAbortException;

}
