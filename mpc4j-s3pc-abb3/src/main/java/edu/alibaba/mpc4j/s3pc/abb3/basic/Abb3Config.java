package edu.alibaba.mpc4j.s3pc.abb3.basic;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongConfig;

/**
 * Configure of Abb party
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public interface Abb3Config extends MultiPartyPtoConfig {
    /**
     * get the config of zl64c party
     */
    TripletLongConfig getZl64cConfig();
    /**
     * get the config of z2c party
     */
    TripletZ2cConfig getZ2cConfig();
    /**
     * get the config of type conversion party
     */
    ConvConfig getConvConfig();
    /**
     * get the config of shuffle party
     */
    ShuffleConfig getShuffleConfig();
}
