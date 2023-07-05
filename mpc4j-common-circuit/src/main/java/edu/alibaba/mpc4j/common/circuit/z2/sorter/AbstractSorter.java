package edu.alibaba.mpc4j.common.circuit.z2.sorter;

import edu.alibaba.mpc4j.common.circuit.z2.AbstractZ2Circuit;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Abstract Sorter
 *
 * @author Li Peng
 * @date 2023/6/12
 */
public abstract class AbstractSorter extends AbstractZ2Circuit implements Sorter {
    /**
     * bit length of input.
     */
    protected int l;
    /**
     * num
     */
    protected int num;
    /**
     * num of elements to be sorted
     */
    protected int sortedNum;

    public AbstractSorter(Z2IntegerCircuit circuit) {
        super(circuit.getParty());
    }

    @Override
    public void sort(MpcZ2Vector[][] xiArrays) throws MpcAbortException {
        this.sortedNum = xiArrays.length;
        this.l = xiArrays[0].length;
        this.num = xiArrays[0][0].getNum();
        sort(xiArrays, party.createOnes(num));
    }
}
