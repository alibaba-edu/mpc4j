package edu.alibaba.mpc4j.common.circuit.prefix;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Prefix tree interface.
 *
 * @author Li Peng
 * @date 2023/10/27
 */
public interface PrefixTree {

    /**
     * Prefix computation using a prefix network.
     *
     * @param l the number of input nodes.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void addPrefix(int l) throws MpcAbortException;
}
