package edu.alibaba.mpc4j.common.tool.benes;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.util.Arrays;

/**
 * 本地实现的贝奈斯网络。（Benes Network）。论文来源：
 *  Chang C, Melhem R. Arbitrary size benes networks[J]. Parallel Processing Letters, 1997, 7(03): 279-284.
 *
 *  实现参考：
 *  https://github.com/osu-crypto/PSI-analytics/blob/master/psi_analytics_eurocrypt19/common/benes.cpp
 *
 * @author Weiran Liu
 * @date 2021/09/26
 */
public class NativeBenesNetwork<T> extends AbstractBenesNetwork<T> {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * 构建本地贝奈斯网络。
     *
     * @param permutationMap 置换表。
     */
    public NativeBenesNetwork(final int[] permutationMap) {
        super(permutationMap);
        // 初始化网络
        byte[][] nativeBenesNetwork = generateBenesNetwork(permutationMap);
        network = Arrays.stream(nativeBenesNetwork)
            .map(row -> {
                boolean[] column = new boolean[row.length];
                for (int i = 0; i < row.length; i++) {
                    column[i] = (row[i] != 0);
                }
                return column;
            })
            .toArray(boolean[][]::new);
    }

    private native byte[][] generateBenesNetwork(int[] permutationMap);

    @Override
    public BenesNetworkFactory.BenesNetworkType getBenesNetworkType() {
        return BenesNetworkFactory.BenesNetworkType.NATIVE_BENES_NETWORK;
    }
}
