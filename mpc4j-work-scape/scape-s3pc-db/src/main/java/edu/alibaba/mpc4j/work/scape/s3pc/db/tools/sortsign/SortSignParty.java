package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.ThreePartyDbPto;

/**
 * interface for sortSign party
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public interface SortSignParty extends ThreePartyDbPto {

    /**
     * set up the usage of functions, and update the tuple info
     *
     * @param params the usage of this function
     * @return [bitTupleNum, longTupleNum]
     */
    long[] setUsage(SortSignFnParam... params);

    /**
     * 输入两个Arithmetic sharing的数据库，然后计算出Key匹配的结果以及对应的置换信息
     *
     * @param leftKeys       输入的左表key
     * @param rightKeys      输入的右表key
     * @param leftValidFlag  the valid indicate flag of left table
     * @param rightValidFlag the valid indicate flag of right table
     * @param inputIsSorted  whether the input is sorted
     * @return [E_1, E_upper, E_down, shuffledId, kPai]
     * E_1代表当前数据和上一行数据都是有效数据，且id不同，key相同
     * E_upper代表当前数据和上一行数据都是有效数据，且key相同
     * E_down代表当前数据和下一行数据都是有效数据，且key相同
     * shuffledId代表置换之后的table标识位，当前数据来自左表则为0，来自右表则为1。
     * kPai是两个表格左表数据在前右表数据在后拼接在一起之后，排序key attribute得到的置换
     */
    TripletLongVector[] preSort(TripletLongVector[] leftKeys, TripletLongVector[] rightKeys,
                                TripletLongVector leftValidFlag, TripletLongVector rightValidFlag, boolean inputIsSorted) throws MpcAbortException;
}
