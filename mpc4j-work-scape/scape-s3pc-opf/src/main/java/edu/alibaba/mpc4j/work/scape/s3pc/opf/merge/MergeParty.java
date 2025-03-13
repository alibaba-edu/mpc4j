package edu.alibaba.mpc4j.work.scape.s3pc.opf.merge;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.ThreePartyOpfPto;

/**
 * the party that merges two sorted inputs
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public interface MergeParty extends ThreePartyOpfPto {
    /**
     * set up the usage of this function
     *
     * @param params the parameters indicating the function and parameters used on one invocation
     */
    long[] setUsage(MergeFnParam... params);

    /**
     * merge two sorted inputs into one sorted outputs
     *
     * @param first  first input
     * @param second second input
     */
    TripletZ2Vector[] merge(TripletZ2Vector[] first, TripletZ2Vector[] second) throws MpcAbortException;

}
