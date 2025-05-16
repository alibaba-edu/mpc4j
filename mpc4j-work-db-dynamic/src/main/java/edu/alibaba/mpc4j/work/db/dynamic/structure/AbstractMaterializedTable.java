package edu.alibaba.mpc4j.work.db.dynamic.structure;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;

/**
 * abstract Materialized Table
 *
 * @author Feng Han
 * @date 2025/3/6
 */
public abstract class AbstractMaterializedTable implements MaterializedTable {
    /**
     * the type of the Materialized Table
     */
    private final MaterializedTableType materializedTableType;
    /**
     * whether the current table is the output table
     */
    private final boolean isOutputTable;
    /**
     * data in Materialized Table, stored in the column-form
     */
    private MpcZ2Vector[] data;
    /**
     * the index of the validity attribute
     */
    private final int validityIndex;

    public AbstractMaterializedTable(MaterializedTableType materializedTableType, MpcZ2Vector[] data,
                                     int validityIndex, boolean isOutputTable) {
        this.materializedTableType = materializedTableType;
        this.data = data;
        this.validityIndex = validityIndex;
        this.isOutputTable = isOutputTable;
    }

    @Override
    public MaterializedTableType getMaterializedTableType() {
        return materializedTableType;
    }

    @Override
    public int getValidityIndex() {
        return validityIndex;
    }

    @Override
    public MpcZ2Vector[] getData() {
        return data;
    }

    @Override
    public void setColumnData(MpcZ2Vector columnData, int targetDim){
        this.data[targetDim] = columnData;
    }

    @Override
    public void updateData(MpcZ2Vector[] data) {
        this.data = data;
    }

    @Override
    public boolean isOutputTable() {
        return isOutputTable;
    }
}
