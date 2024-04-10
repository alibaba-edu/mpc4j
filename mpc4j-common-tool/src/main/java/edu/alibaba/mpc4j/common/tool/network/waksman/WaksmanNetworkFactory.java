package edu.alibaba.mpc4j.common.tool.network.waksman;

import edu.alibaba.mpc4j.common.tool.EnvType;

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
}
