package edu.alibaba.mpc4j.work.db.sketch.SS;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.db.sketch.SketchPartyPto;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;

/**
 * MG computing party interface
 */
public interface SSParty extends SketchPartyPto {
    /**
     * update the sketch or add into buffer
     *
     * @param mgTable cms table
     * @param newData  new data
     * @throws MpcAbortException the protocol failure abort exception
     */
    void update(SketchTable mgTable, MpcVector[] newData) throws MpcAbortException;

    /**
     * get the result of top k query
     *
     * @param mgTable cms table
     * @param k query data
     * @return query result [keys, values]
     * @throws MpcAbortException the protocol failure abort exception
     */
    MpcVector[] getQuery(SketchTable mgTable, int k) throws MpcAbortException;
}
