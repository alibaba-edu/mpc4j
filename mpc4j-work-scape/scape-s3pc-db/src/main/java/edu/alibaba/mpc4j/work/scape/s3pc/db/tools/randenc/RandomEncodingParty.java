package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.ThreePartyDbPto;

/**
 * interface for random encoding party.
 *
 * @author Feng Han
 * @date 2025/2/25
 */
public interface RandomEncodingParty extends ThreePartyDbPto {
    /**
     * set up the usage of functions, and update the tuple info
     *
     * @param params the usage of this function
     * @return [bitTupleNum, longTupleNum]
     */
    long[] setUsage(RandomEncodingFnParam... params);

    /**
     * 根据key的值得到random encoding的值，如果key的长度过长，就通过一个matrix进行hash
     *
     * @param leftKeys  参与join的key
     * @param leftFlag  对应输入的dummy flag
     * @param rightKeys 参与join的key
     * @param rightFlag 对应输入的dummy flag
     * @param withDummy 是否需要pad key，对应的情况是如果输入的key里面可能存在dummy的，那么为了避免encoding的输出不唯一，需要用index来改变
     * @return lowMcRes, lowMcInput
     */
    TripletZ2Vector[][] getEncodingForTwoKeys(TripletZ2Vector[] leftKeys, TripletZ2Vector leftFlag,
                                              TripletZ2Vector[] rightKeys, TripletZ2Vector rightFlag, boolean withDummy) throws MpcAbortException;
}
