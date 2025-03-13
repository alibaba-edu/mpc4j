package edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupParty;

/**
 * group sum party
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public interface GroupSumParty extends GroupParty {

    /**
     * group sum
     *
     * @param input     the input 需要进行极值运算的属性集合
     * @param groupFlag the group flag 如果当前这个是第一个同一个group的行数据，或者是一个dummy record则为0，否则为1
     * @return the result  返回的结果是[计算结果, 以及标志位]
     */
    TripletLongVector[] groupSum(TripletLongVector[] input, TripletLongVector groupFlag) throws MpcAbortException;
}
