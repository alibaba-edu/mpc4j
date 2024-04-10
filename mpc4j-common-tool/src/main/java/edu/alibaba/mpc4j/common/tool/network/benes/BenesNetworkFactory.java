package edu.alibaba.mpc4j.common.tool.network.benes;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Benes network factory.
 *
 * @author Weiran Liu
 * @date 2024/3/20
 */
public class BenesNetworkFactory {
    /**
     * private constructor.
     */
    private BenesNetworkFactory() {
        // empty
    }

    /**
     * Benes network type.
     */
    public enum BenesNetworkType {
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
    public static <X> BenesNetwork<X> createInstance(BenesNetworkType type, int[] permutationMap) {
        switch (type) {
            case JDK:
                return new JdkBenesNetwork<>(permutationMap);
            case NATIVE:
                return new NativeBenesNetwork<>(permutationMap);
            default:
                throw new IllegalArgumentException("Invalid " + BenesNetworkType.class.getSimpleName() + ": " + type.name());
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
    public static <X> BenesNetwork<X> createInstance(BenesNetworkType type, int n, byte[][] network) {
        switch (type) {
            case JDK:
                return new JdkBenesNetwork<>(n, network);
            case NATIVE:
                return new NativeBenesNetwork<>(n, network);
            default:
                throw new IllegalArgumentException("Invalid " + BenesNetworkType.class.getSimpleName() + ": " + type.name());
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
    public static <X> BenesNetwork<X> createInstance(EnvType envType, int[] permutationMap) {
        switch (envType) {
            case STANDARD_JDK:
            case INLAND_JDK:
                return new JdkBenesNetwork<>(permutationMap);
            case STANDARD:
            case INLAND:
                return new NativeBenesNetwork<>(permutationMap);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
