package edu.alibaba.mpc4j.common.tool.network;

import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkFactory.PermutationNetworkType;

import java.util.Vector;
import java.util.stream.IntStream;

/**
 * permutation network.
 *
 * @author Weiran Liu
 * @date 2024/3/20
 */
public interface PermutationNetwork<T> {
    /**
     * Gets type.
     *
     * @return type.
     */
    PermutationNetworkType getType();

    /**
     * Gets the number of levels in the network.
     *
     * @return the number of levels in the network.
     */
    int getLevel();

    /**
     * Gets the maximal width of the network, i.e., the maximal number of switching gate for all levels.
     *
     * @return the maximal width of the network.
     */
    int getMaxWidth();

    /**
     * Gets the width, i.e., the number of switching gate, of the {@code levelIndex}-th column.
     *
     * @param levelIndex the level index.
     * @return the width of the {@code levelIndex}-th column.
     */
    int getWidth(int levelIndex);

    /**
     * Gets the {@code levelIndex}-th column of the network, where
     * <li>0 is identity (a switching gate with binary value 0).</li>
     * <li>1 is switch (a switching gate with binary value 1).</li>
     * <li>2 is empty (no switching gate).</li>
     *
     * @param levelIndex the level index.
     * @return the {@code levelIndex}-th column of the network.
     */
    byte[] getGates(int levelIndex);

    /**
     * Gets the total number of inputs for the network.
     *
     * @return the total number of inputs for the network.
     */
    int getN();

    /**
     * Permutes the input vector using the network.
     *
     * @param inputVector the input vector.
     * @return the permuted input vector.
     */
    Vector<T> permutation(final Vector<T> inputVector);

    /**
     * Gets total number of switches.
     *
     * @return total number of switches.
     */
    default int getSwitchCount() {
        return IntStream.range(0, getLevel())
            .map(this::getWidth)
            .sum();
    }
}
