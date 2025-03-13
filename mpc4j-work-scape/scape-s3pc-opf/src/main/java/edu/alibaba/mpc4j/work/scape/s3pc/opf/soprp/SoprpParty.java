package edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.ThreePartyOpfPto;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpOperations.PrpFnParam;

/**
 * Interface for three-party soprp
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public interface SoprpParty extends ThreePartyOpfPto {
    /**
     * initialize the party
     *
     * @param key set the key
     */
    void init(TripletZ2Vector key) throws MpcAbortException;

    /**
     * set up the usage of this function
     *
     * @param params the parameters indicating the function and parameters used on one invocation
     */
    long[] setUsage(PrpFnParam... params);

    /**
     * get the dimension of input
     */
    int getInputDim();

    /**
     * set the prp parameters
     *
     * @param key the new
     */
    void setKey(TripletZ2Vector key) throws MpcAbortException;

    /**
     * get the prp of input shared data
     *
     * @param xiArrays input shared data
     * @throws MpcAbortException the protocol failure aborts.
     */
    TripletZ2Vector[] enc(TripletZ2Vector[] xiArrays) throws MpcAbortException;

    /**
     * get the invPrp of input shared value
     *
     * @param xiArrays  input shared data
     * @param bitLength output bit length
     * @throws MpcAbortException the protocol failure aborts.
     */
    TripletZ2Vector[] dec(TripletZ2Vector[] xiArrays, int bitLength) throws MpcAbortException;
}
