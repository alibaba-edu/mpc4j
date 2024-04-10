package edu.alibaba.mpc4j.common.tool.network.benes;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkFactory.PermutationNetworkType;
import edu.alibaba.mpc4j.common.tool.network.benes.BenesNetworkFactory.BenesNetworkType;

/**
 * native Benes Network. The implementation is inspired by:
 * <p>https://github.com/osu-crypto/PSI-analytics/blob/master/psi_analytics_eurocrypt19/common/benes.cpp</p>
 *
 * @author Weiran Liu
 * @date 2024/3/20
 */
class NativeBenesNetwork<T> extends AbstractBenesNetwork<T> {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * Creates a network.
     *
     * @param permutationMap permutation map.
     */
    NativeBenesNetwork(final int[] permutationMap) {
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
    NativeBenesNetwork(int n, final byte[][] network) {
        super(n, network);
    }

    private native byte[][] generateBenesNetwork(int[] permutationMap);

    @Override
    public BenesNetworkType getBenesType() {
        return BenesNetworkType.NATIVE;
    }

    @Override
    public PermutationNetworkType getType() {
        return PermutationNetworkType.BENES_NATIVE;
    }
}
