package edu.alibaba.mpc4j.common.tool.network.benes;

import edu.alibaba.mpc4j.common.tool.network.PermutationNetwork;
import edu.alibaba.mpc4j.common.tool.network.benes.BenesNetworkFactory.BenesNetworkType;

/**
 * Benes network, proposed in the following paper by V. Benes for the input size N = 2^n.
 * <p>
 * V. Benes. Permutation groups, complexes, and rearrangeable multistage connecting networks. Bell System Technical
 * Journal, 43:1619-1640, 1964.
 * </p>
 * The Benes network that supports arbitrary size N is proposed by C. Chang and R. Melhem in the following paper:
 * <p>
 * C. Chang, and R. Melhem. Arbitrary size Benes networks. Parallel Processing Letters, 7(3), pp. 279-284, 1997.
 * </p>
 * Benes network gives a solution for permutation using n · log_2(n) - n / 2 switching gates.
 *
 * @author Weiran Liu
 * @date 2024/3/20
 */
public interface BenesNetwork<T> extends PermutationNetwork<T> {
    /**
     * Gets Benes type.
     *
     * @return Benes type.
     */
    BenesNetworkType getBenesType();
}
