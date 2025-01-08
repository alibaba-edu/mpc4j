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
     * Gets layer switch indexes for all wires in that layer. For each layer of the w switches, we label each switch
     * with an index from 0 to w - 1, and each switch takes two wires as inputs. However, some wires do not connect to
     * any switch. This only happens when the permutation size is an odd number. This method returns an array that maps
     * each wire to its connected switch labeled with the switch index in that layer. If the wire does not connect to any
     * switch, the corresponding switch index is -1.
     * <p>
     * For example, given the permutation with size 9. We have 7 layer, and each layer has 4 switches with 8 input/output
     * wires. Each layer has 9 input wires, this means in each layer, there is one wire that does not connect to any
     * switches. Specifically,
     * <ul>
     * <li>8-th wire in the 0-th layer</li>
     * <li>8-th wire in the 1-th layer</li>
     * <li>8-th wire in the 2-th layer</li>
     * <li>6-th wire in the 3-th layer</li>
     * <li>8-th wire in the 4-th layer</li>
     * <li>8-th wire in the 5-th layer</li>
     * <li>8-th wire in the 6-th layer</li>
     * </ul>
     * do not correspond to any switches. Therefore, the value corresponding to these positions are -1. Other positions
     * are switch indexes for the wires in this layer.
     *
     * @return layer switch indexes for all wires in that layer.
     */
    int[][] getLayerSwitchIndexes();

    /**
     * Gets fixed permutations for wires from the previous layer to the current layer. The permutation network is
     * defined by switches together with fixed permutations from the previous layer to the current layer. This method
     * returns these fixed permutations. The 0-th connection (without any previous layer) is the identity permutation.
     * <p>
     * For example, given the permutation with size 9. We have 7 layers, each layer is connected to the previous layer
     * as
     * <ul>
     * <li>0 1 2 3 4 5 6 7 8</li>
     * <li>0 2 4 6 1 3 5 7 8</li>
     * <li>0 2 1 3 4 6 5 7 8</li>
     * <li>0 1 2 3 4 5 6 7 8</li>
     * <li>0 1 2 3 4 5 6 7 8</li>
     * <li>0 2 1 3 4 6 5 7 8</li>
     * <li>0 4 1 5 2 6 3 7 8</li>
     * </ul>
     *
     * @return fixed permutations for wires from the previous layer to the current layer.
     */
    int[][] getFixedLayerPermutations();

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
