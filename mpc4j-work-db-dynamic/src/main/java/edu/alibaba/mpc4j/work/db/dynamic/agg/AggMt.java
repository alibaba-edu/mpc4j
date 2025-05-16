package edu.alibaba.mpc4j.work.db.dynamic.agg;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.work.db.dynamic.structure.AbstractMaterializedTable;
import edu.alibaba.mpc4j.work.db.dynamic.structure.AggregateEnum;
import edu.alibaba.mpc4j.work.db.dynamic.structure.MaterializedTableType;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;

/**
 * aggregate materialized table
 *
 * @author Feng Han
 * @date 2025/3/10
 */
public class AggMt extends AbstractMaterializedTable {
    /**
     * the function of the aggregation
     */
    private final AggregateEnum aggType;
    /**
     * value indexes
     */
    private final int[] valueIndexes;

    public AggMt(MpcZ2Vector[] data, int validityIndex, AggregateEnum aggType) {
        super(MaterializedTableType.GLOBAL_AGG_MT, data, validityIndex, true);
        this.aggType = aggType;
        TIntList list = new TIntLinkedList();
        for (int i = 0; i < data.length; i++) {
            if (i != validityIndex) {
                list.add(i);
            }
        }
        valueIndexes = list.toArray();
    }

    public AggregateEnum getAggType() {
        return aggType;
    }

    public int[] getValueIndexes() {
        return valueIndexes;
    }
}
