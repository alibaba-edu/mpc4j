package edu.alibaba.mpc4j.work.db.sketch.HLL;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.db.sketch.SketchPartyPto;

public interface HLLParty extends SketchPartyPto {

    /**
     * update a batch of elements, the number can be arbitrary.
     *
     * @param elements: a list of elements
     * @Exception: comes from lowmc.enc.
     */
    void update(AbstractHLLTable hllTable, MpcVector[] elements) throws MpcAbortException;

    /**
     * no input needed
     *
     * @return the sum of all the counters
     */
    TripletZ2Vector[] query(AbstractHLLTable hllTable) throws MpcAbortException;
}
