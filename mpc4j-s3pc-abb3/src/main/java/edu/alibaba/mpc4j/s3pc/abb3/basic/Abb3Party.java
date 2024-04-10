package edu.alibaba.mpc4j.s3pc.abb3.basic;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;

/**
 * Interface for abb3 Party
 *
 * @author Feng Han
 * @date 2023/12/15
 */
public interface Abb3Party extends ThreePartyPto {
    /**
     * update the number of tuples used in verification
     *
     * @param bitTupleNum  the number of bit tuples
     * @param longTupleNum the number of zl64 tuples
     */
    void updateNum(long bitTupleNum, long longTupleNum);
    /**
     * initialize the party
     */
    void init();
    /**
     * verify the multiplication operations
     */
    void checkUnverified() throws MpcAbortException;
    /**
     * get TripletProvider
     */
    TripletProvider getTripletProvider();
    /**
     * get the z2c party
     */
    TripletZ2cParty getZ2cParty();
    /**
     * get the zl64c party
     */
    TripletLongParty getLongParty();
    /**
     * get the type conversion party
     */
    ConvParty getConvParty();
    /**
     * get the shuffle party
     */
    ShuffleParty getShuffleParty();
}
