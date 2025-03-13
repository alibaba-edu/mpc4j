package edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeFactory.ExtremeType;

/**
 * group extreme party
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public interface GroupExtremeParty extends GroupParty {
    /**
     * group extreme
     *
     * @param input       the input 需要进行极值运算的属性集合
     * @param groupFlag   the group flag 如果当前这个是第一个同一个group的行数据，或者是一个dummy record则为0，否则为1
     * @param extremeType the extreme type 需要得到的是max？min？
     * @return the result  返回的结果是[计算结果, 以及标志位]
     */
    TripletZ2Vector[] groupExtreme(TripletZ2Vector[] input, TripletZ2Vector groupFlag, ExtremeType extremeType) throws MpcAbortException;
}
