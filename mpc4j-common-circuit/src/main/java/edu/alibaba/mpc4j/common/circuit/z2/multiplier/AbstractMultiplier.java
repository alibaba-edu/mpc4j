package edu.alibaba.mpc4j.common.circuit.z2.multiplier;

import edu.alibaba.mpc4j.common.circuit.z2.AbstractZ2Circuit;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;

/**
 * Abstract Multiplier.
 *
 * @author Li Peng
 * @date 2023/6/6
 */
public abstract class AbstractMultiplier extends AbstractZ2Circuit implements Multiplier {
    /**
     * bit length of input.
     */
    protected int l;
    /**
     * num
     */
    protected int num;

    public AbstractMultiplier(MpcZ2cParty party) {
        super(party);
    }
}
