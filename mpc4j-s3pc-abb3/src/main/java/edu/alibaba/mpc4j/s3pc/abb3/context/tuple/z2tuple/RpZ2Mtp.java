package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env.RpZ2EnvParty;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;

/**
 * interface of z2 mtp for replicated 3p sharing
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public interface RpZ2Mtp extends ThreePartyPto {
    /**
     * the total tuples needed
     */
    void init(long totalBit);

    /**
     * get the environment for generating z2 tuples
     */
    RpZ2EnvParty getEnv();

    /**
     * return the required tuples
     *
     * @param bitNums how many bits are needed in each tuple vector
     */
    TripletRpZ2Vector[][] getTuple(int[] bitNums) throws MpcAbortException;

    /**
     * return the actually used tuples
     */
    long getAllTupleNum();
}
