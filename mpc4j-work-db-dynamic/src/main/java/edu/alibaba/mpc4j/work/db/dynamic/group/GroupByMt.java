package edu.alibaba.mpc4j.work.db.dynamic.group;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.work.db.dynamic.structure.AbstractMaterializedTable;
import edu.alibaba.mpc4j.work.db.dynamic.structure.AggregateEnum;
import edu.alibaba.mpc4j.work.db.dynamic.structure.MaterializedTableType;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Materialized Table for group-by
 *
 * @author Feng Han
 * @date 2025/3/6
 */
public class GroupByMt extends AbstractMaterializedTable {
    /**
     * the indexes of group-by key attribute
     */
    private final int[] groupKeyIndexes;
    /**
     * the indexes of group-by-agg value attribute
     */
    private final int[] valueIndexes;
    /**
     * aggregate type
     */
    private final AggregateEnum aggType;

    public GroupByMt(MpcZ2Vector[] data, int validityIndex, boolean isOutputTable, int[] groupKeyIndexes, AggregateEnum aggType) {
        super(MaterializedTableType.GROUP_BY_MT, data, validityIndex, isOutputTable);
        this.groupKeyIndexes = groupKeyIndexes;
        this.aggType = aggType;
        TIntList list = new TIntLinkedList();
        TIntSet keySet = new TIntHashSet(Arrays.stream(groupKeyIndexes).boxed().collect(Collectors.toList()));
        for (int i = 0; i < data.length; i++) {
            if(!(keySet.contains(i) || i== validityIndex)) {
                list.add(i);
            }
        }
        valueIndexes = list.toArray();
    }

    public int[] getGroupKeyIndexes() {
        return groupKeyIndexes;
    }

    public int[] getValueIndexes() {
        return valueIndexes;
    }

    public AggregateEnum getAggType() {
        return aggType;
    }
}
