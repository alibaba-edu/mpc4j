package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env.RpZ2EnvParty;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;

/**
 * interface of replicated 3p sharing z2 mt generator
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public interface RpZ2Mtg extends ThreePartyPto {
    /**
     * the total tuples needed
     */
    void init(long totalBit);
    /**
     * get the log of round for generate all tuples
     */
    int getLogOfRound();

    /**
     * get the environment for generating z2 tuples
     */
    RpZ2EnvParty getEnv();

    /**
     * return the generated tuples
     */
    TripletRpZ2Vector[][] genMtOnline() throws MpcAbortException;
}
