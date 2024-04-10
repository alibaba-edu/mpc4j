package edu.alibaba.mpc4j.s3pc.abb3.basic.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;

/**
 * Abstract party for three-party secret sharing, in order to verify computation before open or reveal
 *
 * @author Feng Han
 * @date 2024/01/26
 */
public interface AbbCoreParty extends ThreePartyPto {
    /**
     * get the TripletProvider
     */
    TripletProvider getTripletProvider();
    /**
     * verify and result
     *
     * @throws MpcAbortException if the protocol is abort.
     */
    void verifyMul() throws MpcAbortException;

    /**
     * get the flag representing whether the current party is currently in the verification process
     */
    boolean getDuringVerificationFlag();

    /**
     * set the flag representing whether the current party is currently in the verification process
     */
    void setDuringVerificationFlag(boolean duringVerificationFlag);
}
