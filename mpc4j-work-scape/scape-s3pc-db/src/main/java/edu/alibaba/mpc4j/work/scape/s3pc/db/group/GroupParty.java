package edu.alibaba.mpc4j.work.scape.s3pc.db.group;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.ThreePartyDbPto;

/**
 * interface for group operation
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public interface GroupParty extends ThreePartyDbPto {
    /**
     * set the usage of the protocol
     *
     * @param params the input parameters
     * @return the required tuple number
     */
    long[] setUsage(GroupFnParam... params);

    /**
     * 用来得到groupSign，如果当前这个是第一个同一个group的行数据，或者是一个dummy record则为0，否则为1
     * @param input 输入的表格数据
     * @param keyIndex 哪些是group的key
     */
    TripletLongVector getGroupFlag(TripletLongVector[] input, int[] keyIndex) throws MpcAbortException;

    /**
     * 用来得到groupSign，如果当前这个是第一个同一个group的行数据，或者是一个dummy record则为0，否则为1
     * @param input 输入的表格数据
     * @param keyIndex 哪些是group的key
     */
    TripletZ2Vector getGroupFlag(TripletZ2Vector[] input, int[] keyIndex) throws MpcAbortException;

}
