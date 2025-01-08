package edu.alibaba.mpc4j.common.tool.network.benes;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

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
            case STANDARD:
            case INLAND:
                return new JdkBenesNetwork<>(permutationMap);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }

    /**
     * Gets total number of switches. The formula is shown in Section 4 of the paper.
     * <p>S(k) = 2⌊k / 2⌋ + S(⌈k / 2⌉) + S(⌊k / 2⌋)</p>
     *
     * @return total number of switches.
     */
    public static int getSwitchCount(int n) {
        MathPreconditions.checkGreater("n", n, 1);
        return innerGetSwitchCount(n);
    }

    private static int innerGetSwitchCount(int n) {
        // in inner function, we allow n = 1 since this condition can be reached in recursion。
        if (n == 1) {
            // S(1) = 0
            return 0;
        }
        if (n == 2) {
            // S(2) = 1
            return 1;
        } else {
            if (n % 2 == 0) {
                return 2 * (n / 2) + innerGetSwitchCount(n / 2) + innerGetSwitchCount(n / 2);
            } else {
                return 2 * (n / 2) + innerGetSwitchCount(n / 2 + 1) + innerGetSwitchCount(n / 2);
            }
        }
    }
}
