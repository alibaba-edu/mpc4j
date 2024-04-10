package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env.RpLongEnvParty;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

/**
 * interface of replicated 3p sharing zl64 mt generator
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public interface RpLongMtg extends ThreePartyPto {
    /**
     * the total tuples needed
     */
    void init(long totalData);

    /**
     * get the log of round for generate all tuples
     */
    int getLogOfRound();

    /**
     * get the environment for generating zl64 tuples
     */
    RpLongEnvParty getEnv();

    /**
     * return the generated tuples
     */
    TripletRpLongVector[][] genMtOnline() throws MpcAbortException;
}
