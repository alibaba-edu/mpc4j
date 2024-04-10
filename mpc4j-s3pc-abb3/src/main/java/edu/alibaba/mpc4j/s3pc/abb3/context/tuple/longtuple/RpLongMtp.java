package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env.RpLongEnvParty;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

/**
 * interface of zl64 mtp for replicated 3p sharing
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public interface RpLongMtp extends ThreePartyPto {
    /**
     * the total tuples needed
     */
    void init(long totalData);

    /**
     * get the environment for generating zl64 tuples
     */
    RpLongEnvParty getEnv();

    /**
     * return the required tuples
     *
     * @param nums how many elements are needed in each tuple vector
     */
    TripletRpLongVector[][] getTuple(int[] nums) throws MpcAbortException;

    /**
     * return the actually used tuples
     */
    long getAllTupleNum();
}
