package edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.ThreePartyDbPto;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.SemiJoinFnParam;

/**
 * interface of the general semi-join party
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public interface GeneralSemiJoinParty extends ThreePartyDbPto {
    /**
     * compute the required tuple number for all operations
     *
     * @param params input information of the PkPk join protocol
     * @return [bitTupleNum, longTupleNum]
     */
    long[] setUsage(SemiJoinFnParam... params);

    /**
     * 输入两个Arithmetic sharing的数据库，然后计算出Key匹配的结果以及对应的置换信息
     *
     * @param x         输入的左表
     * @param y         输入的右表
     * @param xKeyIndex 左表中key所在的index
     * @param yKeyIndex 右表中key所在的index
     * @param inputIsSorted the input is sorted in the order of join_key and valid_flag
     * @return 一个新的f，表示了右表y的semi-join的结果
     */
    TripletLongVector semiJoin(TripletLongVector[] x, TripletLongVector[] y,
                               int[] xKeyIndex, int[] yKeyIndex, boolean inputIsSorted) throws MpcAbortException;
}
