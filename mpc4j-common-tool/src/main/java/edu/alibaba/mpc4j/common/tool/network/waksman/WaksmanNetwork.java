package edu.alibaba.mpc4j.common.tool.network.waksman;

import edu.alibaba.mpc4j.common.tool.network.PermutationNetwork;
import edu.alibaba.mpc4j.common.tool.network.waksman.WaksmanNetworkFactory.WaksmanNetworkType;

/**
 * Waksman network, proposed in the following paper by A. Waksman for the input size N = 2^n.
 * <p>
 * A. Waksman. A permutation network. Journal of the ACM, 15: 159-163, 1969.
 * </p>
 * The Waksman network that supports arbitrary size N is proposed by B. Beauquier and E. Darrot in the following paper:
 * <p>
 * B. Beauquier, E. Darrot. On arbitrary Waksman networks and their vulnerability. Parallel Processing Letters, 12: 287-296.
 * </p>
 * Benes network gives an improved solution compared with Benes network for permutation using N · log_2(N) - N + 1
 * switching gates (for N = 2^n).
 *
 * @author Weiran Liu
 * @date 2024/3/21
 */
public interface WaksmanNetwork<T> extends PermutationNetwork<T> {
    /**
     * Gets Waksman type.
     *
     * @return Waksman type.
     */
    WaksmanNetworkType getWaksmanType();
}
