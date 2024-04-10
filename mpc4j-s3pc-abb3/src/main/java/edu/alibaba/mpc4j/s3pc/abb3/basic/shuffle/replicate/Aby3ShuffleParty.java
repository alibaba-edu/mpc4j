package edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate;

import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;

/**
 * Interface for three-party replicated-sharing shuffling
 *
 * @author Feng Han
 * @date 2024/01/18
 */
public interface Aby3ShuffleParty extends ShuffleParty {
    /**
     * get the provider
     */
    TripletProvider getProvider();
    /**
     * get the Z2cParty
     */
    TripletZ2cParty getZ2cParty();
    /**
     * get the Zl64cParty
     */
    TripletLongParty getZl64cParty();
}
