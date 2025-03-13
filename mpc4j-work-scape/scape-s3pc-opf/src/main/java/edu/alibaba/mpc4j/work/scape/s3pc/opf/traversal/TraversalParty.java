package edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal;

import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.ThreePartyOpfPto;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalOperations.TraversalFnParam;

/**
 * Interface for three-party traversal
 *
 * @author Feng Han
 * @date 2024/02/28
 */
public interface TraversalParty extends ThreePartyOpfPto {

    /**
     * set up the usage of this function
     *
     * @param params the parameters indicating the function and parameters used on one invocation
     */
    long[] setUsage(TraversalFnParam... params);

    /**
     * 输入的最后一维做为标识位，标识是否为有效值 flag
     * 方法是BrentKung network
     * when !isInv:
     * if flag[i] == 0: result[i] = value[i]
     * else:            result[i] = result[i-1] + (1 - theta) · value[i]
     * when isInv
     * if flag[i] == 0: result[i] = value[i]
     * else:            result[i] = result[i+1] + (1 - theta) · value[i]
     *
     * @param input      输入的数据，输入的最后一维标识是否为有效值 flag
     * @param isInv      是不是逆序遍历的
     * @param keepFlag   是否也将flag一起遍历
     * @param keepOrigin 是否保留原始的数据
     * @param theta      计算函数的参数
     */
    TripletLongVector[] traversalPrefix(TripletLongVector[] input, boolean isInv, boolean keepFlag, boolean keepOrigin, boolean theta);

    /**
     * 遍历执行or运算,如果group里面的dummyFlag都是0， 那么一个group中最后一个元素的结果就是0，否则一个group中最后一个元素的结果就是1
     * 方法是BrentKung network
     * when !isInv:
     * if flag[i] == 0: result[i] = value[i]
     * else:            result[i] = result[i-1] or value[i]
     * when isInv
     * if flag[i] == 0: result[i] = value[i]
     * else:            result[i] = result[i+1] or value[i]
     *
     * @param dummyFlag  输入的数据
     * @param isInv      是不是逆序遍历的
     * @param keepOrigin 是否保留原始的数据
     * @param groupFlag  如果当前这个是第一个同一个group的行数据，或者是一个dummy record则为0，否则为1
     */
    TripletZ2Vector[] traversalPrefix(TripletZ2Vector[] dummyFlag, TripletZ2Vector groupFlag, boolean keepOrigin, boolean isInv);
}
