package edu.alibaba.mpc4j.work.db.sketch.utils.pop;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.ThreePartyOpfPto;

/**
 * pop party interface
 */
public interface PopParty extends ThreePartyOpfPto {
    /**
     * set the usage of the protocol
     *
     * @param params the input parameters
     * @return the required tuple number
     */
    long[] setUsage(PopFnParam... params);

    /**
     * pop, the remaining values
     *
     * @param input Input table
     * @param index target index indicating which element should be popped
     */
    TripletZ2Vector[] pop(TripletZ2Vector[] input, TripletZ2Vector[] index) throws MpcAbortException;

    /**
     * pop, the remaining values
     *
     * @param input Input table
     * @param flag  indicator flag, indicating which element should be popped
     */
    TripletZ2Vector[] pop(TripletZ2Vector[] input, TripletZ2Vector flag);
}
