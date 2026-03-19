package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.db.sketch.SketchPartyPto;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;

/**
 * CMS computing party interface
 */
public interface CMSParty extends SketchPartyPto {
    /**
     * update the sketch or add into buffer
     *
     * @param cmsTable cms table
     * @param newData  new data
     * @throws MpcAbortException the protocol failure abort exception
     */
    void update(SketchTable cmsTable, MpcVector[] newData) throws MpcAbortException;

    /**
     * get the result of point query
     *
     * @param cmsTable cms table
     * @param queryData query data
     * @return query
     * @throws MpcAbortException the protocol failure abort exception
     */
    MpcVector[] getQuery(SketchTable cmsTable, MpcVector[] queryData) throws MpcAbortException;
}
