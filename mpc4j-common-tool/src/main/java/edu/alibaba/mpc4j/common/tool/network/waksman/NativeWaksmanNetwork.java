package edu.alibaba.mpc4j.common.tool.network.waksman;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkFactory.PermutationNetworkType;
import edu.alibaba.mpc4j.common.tool.network.waksman.WaksmanNetworkFactory.WaksmanNetworkType;

/**
 * native Waksman network.
 *
 * @author Weiran Liu
 * @date 2024/3/22
 */
class NativeWaksmanNetwork<T> extends AbstractWaksmanNetwork<T> {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * Creates a network.
     *
     * @param permutationMap permutation map.
     */
    NativeWaksmanNetwork(final int[] permutationMap) {
        super(permutationMap);
        network = generateBenesNetwork(permutationMap);
        updateWidths();
    }

    /**
     * Creates a network by directly setting the network.
     *
     * @param n       number of inputs.
     * @param network network.
     */
    NativeWaksmanNetwork(int n, final byte[][] network) {
        super(n, network);
    }

    private native byte[][] generateBenesNetwork(int[] permutationMap);

    @Override
    public WaksmanNetworkType getWaksmanType() {
        return WaksmanNetworkType.NATIVE;
    }

    @Override
    public PermutationNetworkType getType() {
        return PermutationNetworkType.WAKSMAN_NATIVE;
    }
}
