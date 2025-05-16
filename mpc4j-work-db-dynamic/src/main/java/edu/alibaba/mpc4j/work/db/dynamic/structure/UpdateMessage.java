package edu.alibaba.mpc4j.work.db.dynamic.structure;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;

/**
 * update message, including a row data and an operation in {DELETE, INSERT}
 *
 * @author Feng Han
 * @date 2025/3/6
 */
public class UpdateMessage {
    /**
     * an operation in {DELETE, INSERT}
     */
    private final OperationEnum operation;
    /**
     * data of one row, stored in column-form
     */
    private final MpcZ2Vector[] rowData;

    public UpdateMessage(OperationEnum operation, MpcZ2Vector[] rowData) {
        this.operation = operation;
        this.rowData = rowData;
    }

    public OperationEnum getOperation() {
        return operation;
    }

    public MpcZ2Vector[] getRowData() {
        return rowData;
    }
}
