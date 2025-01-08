package edu.alibaba.mpc4j.common.tool.network.decomposer;

/**
 * Permutation decomposer
 *
 * @author Feng Han
 * @date 2024/8/12
 */
public interface PermutationDecomposer {
    /**
     * Splits vector into groups.
     *
     * @param vector vector.
     * @param i      layer index.
     * @return groups.
     */
    byte[][][] splitVector(byte[][] vector, int i);

    /**
     * Combines groups into vector.
     *
     * @param groups groups.
     * @param i      layer index.
     * @return vector.
     */
    byte[][] combineGroups(byte[][][] groups, int i);

    /**
     * Sets sub-permutations. We have d layers, each contains N / T groups, each group corresponds a [T] → [T]
     * sub-permutations.
     *
     * @param permutation permutation.
     */
    void setPermutation(final int[] permutation);

    /**
     * Permutations.
     *
     * @param inputGroups input groups.
     * @param i           layer index.
     * @return output groups.
     */
    byte[][][] permutation(byte[][][] inputGroups, int i);

    /**
     * Permutations.
     *
     * @param inputVector input vector.
     * @param i           layer index.
     * @return output vector.
     */
    byte[][] permutation(byte[][] inputVector, int i);

    /**
     * Gets N.
     *
     * @return N.
     */
    int getN();

    /**
     * Gets d = 2 * ceil(log(N) / log(T)) - 1, the number of split layers.
     *
     * @return d.
     */
    int getD();

    /**
     * Gets the number of groups in each layer, i.e, N / T.
     *
     * @param level the index of level
     * @return the number of groups in each layer.
     */
    int getG(int level);

    /**
     * Gets T, the sub-permutation size.
     *
     * @param level the index of level
     * @return T.
     */
    int getT(int level);

    /**
     * Gets sub-permutations.
     *
     * @return sub-permutations.
     */
    int[][][] getSubPermutations();
}