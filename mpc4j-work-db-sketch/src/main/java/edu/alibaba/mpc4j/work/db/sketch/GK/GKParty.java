package edu.alibaba.mpc4j.work.db.sketch.GK;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.db.sketch.SketchPartyPto;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;

/**
 * GK computing party interface
 */
public interface GKParty extends SketchPartyPto {
    /**
     * update the sketch or add into buffer
     *
     * @param gkTable gk table
     * @param newData  new data
     * @throws MpcAbortException the protocol failure abort exception
     */
    void update(SketchTable gkTable, MpcVector[] newData) throws MpcAbortException;

    /**
     * get the result of quantile query
     *
     * @param gkTable gk table
     * @return query result
     * @throws MpcAbortException the protocol failure abort exception
     */
    MpcVector[] getQuery(SketchTable gkTable, MpcVector[] queryData) throws MpcAbortException;
}
