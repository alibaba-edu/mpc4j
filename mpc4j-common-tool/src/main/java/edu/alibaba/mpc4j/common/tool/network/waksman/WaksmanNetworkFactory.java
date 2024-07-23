package edu.alibaba.mpc4j.common.tool.network.waksman;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * Waksman network factory.
 *
 * @author Weiran Liu
 * @date 2024/3/21
 */
public class WaksmanNetworkFactory {
    /**
     * private constructor.
     */
    private WaksmanNetworkFactory() {
        // empty
    }

    /**
     * Waksman network type.
     */
    public enum WaksmanNetworkType {
        /**
         * JDK
         */
        JDK,
        /**
         * Native
         */
        NATIVE,
    }

    /**
     * Creates a network.
     *
     * @param type           network type.
     * @param permutationMap permutation map.
     * @param <X>            input type.
     * @return a network.
     */
    public static <X> WaksmanNetwork<X> createInstance(WaksmanNetworkType type, int[] permutationMap) {
        switch (type) {
            case JDK:
                return new JdkWaksmanNetwork<>(permutationMap);
            case NATIVE:
                return new NativeWaksmanNetwork<>(permutationMap);
            default:
                throw new IllegalArgumentException("Invalid " + WaksmanNetwork.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a network.
     *
     * @param type    network type.
     * @param n       number of inputs.
     * @param network network.
     * @param <X>     input type.
     * @return a network.
     */
    public static <X> WaksmanNetwork<X> createInstance(WaksmanNetworkType type, int n, byte[][] network) {
        switch (type) {
            case JDK:
                return new JdkWaksmanNetwork<>(n, network);
            case NATIVE:
                return new NativeWaksmanNetwork<>(n, network);
            default:
                throw new IllegalArgumentException("Invalid " + WaksmanNetwork.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a network.
     *
     * @param envType        environment.
     * @param permutationMap permutation map.
     * @param <X>            input type.
     * @return a network.
     */
    public static <X> WaksmanNetwork<X> createInstance(EnvType envType, int[] permutationMap) {
        switch (envType) {
            case STANDARD_JDK:
            case INLAND_JDK:
                return new JdkWaksmanNetwork<>(permutationMap);
            case STANDARD:
            case INLAND:
                return new NativeWaksmanNetwork<>(permutationMap);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }

    /**
     * Gets total number of switches. The formula is shown in Section 4 of the paper.
     * <p>S(n) = S(⌈n / 2⌉) + S(⌊n / 2⌋) + n - 1</p>
     *
     * @return total number of switches.
     */
    public static int getSwitchCount(int n) {
        MathPreconditions.checkGreater("n", n, 1);
        return innerGetSwitchCount(n);
    }

    private static int innerGetSwitchCount(int n) {
        if (n == 1) {
            // S(1) = 0
            return 0;
        } else {
            if (n % 2 == 0) {
                return innerGetSwitchCount(n / 2) + innerGetSwitchCount(n / 2) + n - 1;
            } else {
                return innerGetSwitchCount(n / 2 + 1) + innerGetSwitchCount(n / 2) + n - 1;
            }
        }
    }
}
