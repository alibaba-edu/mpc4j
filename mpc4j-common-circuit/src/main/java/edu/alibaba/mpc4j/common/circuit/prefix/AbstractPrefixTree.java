package edu.alibaba.mpc4j.common.circuit.prefix;

/**
 * Abstract prefix tree.
 *
 * @author Li Peng
 * @date 2023/10/27
 */
public abstract class AbstractPrefixTree implements PrefixTree {
    /**
     * Prefix sum operation.
     */
    protected final PrefixOp prefixOp;

    public AbstractPrefixTree(PrefixOp prefixOp) {
        this.prefixOp = prefixOp;
    }
}
