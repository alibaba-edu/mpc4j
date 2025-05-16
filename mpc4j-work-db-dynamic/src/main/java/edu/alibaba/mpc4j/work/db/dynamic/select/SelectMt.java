package edu.alibaba.mpc4j.work.db.dynamic.select;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.work.db.dynamic.structure.AbstractMaterializedTable;
import edu.alibaba.mpc4j.work.db.dynamic.structure.MaterializedTableType;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * materialized table for select
 * 要求table中有可以唯一标识一行数据的id存在
 *
 * @author Feng Han
 * @date 2025/3/6
 */
public class SelectMt extends AbstractMaterializedTable {
    /**
     * the indexes of unique id attribute
     */
    private final int[] idIndexes;
    /**
     * the function of the select, the result is a validity attribute
     * the input is the value attributes and a validity attribute, where the validity attribute is the last column
     */
    private final BiFunction<Z2IntegerCircuit, MpcZ2Vector[], MpcZ2Vector> function;
    /**
     * the indexes of group-by-agg value attribute
     */
    private final int[] valueIndexes;

    public SelectMt(MpcZ2Vector[] data, int validityIndex, boolean isOutputTable,
                    int[] idIndexes, BiFunction<Z2IntegerCircuit, MpcZ2Vector[], MpcZ2Vector> function) {
        super(MaterializedTableType.SELECT_MT, data, validityIndex, isOutputTable);
        this.idIndexes = idIndexes;
        this.function = function;
        TIntList list = new TIntLinkedList();
        TIntSet keySet = new TIntHashSet(Arrays.stream(idIndexes).boxed().collect(Collectors.toList()));
        for (int i = 0; i < data.length; i++) {
            if(!(keySet.contains(i) || i== validityIndex)) {
                list.add(i);
            }
        }
        valueIndexes = list.toArray();
    }

    public int[] getIdIndexes() {
        return idIndexes;
    }

    public int[] getValueIndexes() {
        return valueIndexes;
    }

    public BiFunction<Z2IntegerCircuit, MpcZ2Vector[], MpcZ2Vector> getFunction() {
        return function;
    }
}
