package edu.alibaba.mpc4j.work.db.dynamic.structure;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;

/**
 * @author Feng Han
 * @date 2025/3/6
 */
public interface MaterializedTable {
    /**
     * get the type of the Materialized Table
     */
    MaterializedTableType getMaterializedTableType();
    /**
     * whether the current Materialized Table is output table
     */
    boolean isOutputTable();
    /**
     * get the index of validity attribute
     */
    int getValidityIndex();
    /**
     * get the data of the Materialized Table
     */
    MpcZ2Vector[] getData();
    /**
     * update the data of the Materialized Table
     */
    void setColumnData(MpcZ2Vector columnData, int targetDim);
    /**
     * update the data of the Materialized Table
     */
    void updateData(MpcZ2Vector[] data);
}
