package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.z2.AbstractZ2Circuit;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Abstract Adder.
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public abstract class AbstractAdder extends AbstractZ2Circuit implements Adder {
    /**
     * bit length of input.
     */
    protected int l;
    /**
     * num
     */
    protected int num;

    public AbstractAdder(MpcZ2cParty party) {
        super(party);
    }

    @Override
    public MpcZ2Vector[] add(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray, boolean cin)
        throws MpcAbortException {
        this.l = xiArray.length;
        this.num = xiArray[0].getNum();
        int bitNum = xiArray[0].getNum();
        MpcZ2Vector cinVector = party.create(bitNum, cin);
        return add(xiArray, yiArray, cinVector);
    }
}
