package edu.alibaba.mpc4j.common.circuit.z2.psorter;

import edu.alibaba.mpc4j.common.circuit.z2.AbstractZ2Circuit;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;

/**
 * Abstract Sorter for permutation generation
 *
 * @author Feng Han
 * @date 2023/10/30
 */
public abstract class AbstractPermutationSorter extends AbstractZ2Circuit implements Psorter {
    /**
     * bit length of xiArrays.
     */
    protected int[] xls;
    /**
     * bit length of payload.
     */
    protected int[] yls;
    /**
     * num of elements to be sorted
     */
    protected int sortedNum;
    protected boolean needPermutation;
    protected boolean needStable;
    /**
     * Z2 integer circuit.
     */
    protected final Z2IntegerCircuit circuit;

    public AbstractPermutationSorter(Z2IntegerCircuit circuit) {
        super(circuit.getParty());
        this.circuit = circuit;
    }
}
