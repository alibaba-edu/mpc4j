package edu.alibaba.mpc4j.work.db.dynamic.join.pkpk;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.work.db.dynamic.structure.AbstractMaterializedTable;
import edu.alibaba.mpc4j.work.db.dynamic.structure.MaterializedTableType;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * the Materialized Table for input table of a join operation
 *
 * @author Feng Han
 * @date 2025/3/10
 */
public class JoinInputMt extends AbstractMaterializedTable {
    /**
     * the indexes of join_key attribute for the left table
     */
    private final int[] keyIndexes;
    /**
     * the indexes of group-by-agg value attribute
     */
    private final int[] valueIndexes;

    public JoinInputMt(MpcZ2Vector[] data, int validityIndex, int[] keyIndexes) {
        super(MaterializedTableType.JOIN_INPUT_MT, data, validityIndex, false);
        this.keyIndexes = keyIndexes;
        TIntList list = new TIntLinkedList();
        TIntSet keySet = new TIntHashSet(Arrays.stream(keyIndexes).boxed().collect(Collectors.toList()));
        for (int i = 0; i < data.length; i++) {
            if (!(keySet.contains(i) || i == validityIndex)) {
                list.add(i);
            }
        }
        valueIndexes = list.toArray();
    }

    public int[] getKeyIndexes() {
        return keyIndexes;
    }

    public int[] getValueIndexes() {
        return valueIndexes;
    }
}
