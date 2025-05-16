package edu.alibaba.mpc4j.work.db.dynamic.join.pkpk;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.work.db.dynamic.structure.AbstractMaterializedTable;
import edu.alibaba.mpc4j.work.db.dynamic.structure.MaterializedTableType;

/**
 * mt for PK-PK join
 *
 * @author Feng Han
 * @date 2025/3/6
 */
public class PkPkJoinMt extends AbstractMaterializedTable {
    /**
     * the indexes of join_key attribute
     */
    private final int[] keyIndexes;
    /**
     * the indexes of payload attribute for the left table
     */
    private final int[] leftValueIndexes;
    /**
     * the indexes of payload attribute for the right table
     */
    private final int[] rightValueIndexes;
    /**
     * the Materialized Table for left input table of a join operation
     */
    private final JoinInputMt leftMt;
    /**
     * the Materialized Table for right input table of a join operation
     */
    private final JoinInputMt rightMt;

    public PkPkJoinMt(MpcZ2Vector[] data, int validityIndex, boolean isOutputTable,
                      int[] keyIndexes, int[] leftValueIndexes, int[] rightValueIndexes,
                      JoinInputMt leftMt, JoinInputMt rightMt
    ) {
        super(MaterializedTableType.PK_PK_JOIN_MT, data, validityIndex, isOutputTable);
        this.keyIndexes = keyIndexes;
        this.leftValueIndexes = leftValueIndexes;
        this.rightValueIndexes = rightValueIndexes;
        this.leftMt = leftMt;
        this.rightMt = rightMt;
    }

    public int[] getKeyIndexes() {
        return keyIndexes;
    }

    public int[] getLeftValueIndexes() {
        return leftValueIndexes;
    }

    public int[] getRightValueIndexes() {
        return rightValueIndexes;
    }

    public JoinInputMt getLeftMt() {
        return leftMt;
    }

    public JoinInputMt getRightMt() {
        return rightMt;
    }
}
