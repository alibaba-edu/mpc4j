package edu.alibaba.mpc4j.work.db.dynamic.orderby;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.work.db.dynamic.structure.AbstractMaterializedTable;
import edu.alibaba.mpc4j.work.db.dynamic.structure.MaterializedTableType;

/**
 * order-by Materialized Table
 *
 * @author Feng Han
 * @date 2025/3/6
 */
public class OrderByMt extends AbstractMaterializedTable {
    /**
     * the indexes of order-by key attribute
     */
    private final int[] orderKeyIndexes;
    /**
     * the indexes of unique id attribute
     */
    private final int[] idIndexes;
    /**
     * limit number of the order-by-limit query
     */
    private final int limitNum;
    /**
     * deletion threshold of this order-by-limit materialized table
     */
    private final int deletionThreshold;
    /**
     * limit number of the order-by-limit query
     */
    private int currentDeleteNum;

    public OrderByMt(MpcZ2Vector[] data, int validityIndex, boolean isOutputTable, int[] orderKeyIndexes, int[] idIndexes, int limitNum, int deletionThreshold) {
        super(MaterializedTableType.ORDER_BY_MT, data, validityIndex, isOutputTable);
        this.orderKeyIndexes = orderKeyIndexes;
        this.idIndexes = idIndexes;
        this.limitNum = limitNum;
        this.deletionThreshold = deletionThreshold;
        currentDeleteNum = 0;
    }

    public int[] getOrderKeyIndexes() {
        return orderKeyIndexes;
    }

    public int[] getIdIndexes() {
        return idIndexes;
    }

    public int getDeletionThreshold() {
        return deletionThreshold;
    }

    public int getLimitNum() {
        return limitNum;
    }

    public int getCurrentDeleteNum() {
        return currentDeleteNum;
    }

    public void increaseDeleteNum() {
        currentDeleteNum++;
    }
}
