package edu.alibaba.mpc4j.common.tool.network;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.network.benes.BenesNetworkFactory;
import edu.alibaba.mpc4j.common.tool.network.benes.BenesNetworkFactory.BenesNetworkType;
import edu.alibaba.mpc4j.common.tool.network.waksman.WaksmanNetworkFactory;
import edu.alibaba.mpc4j.common.tool.network.waksman.WaksmanNetworkFactory.WaksmanNetworkType;

/**
 * permutation network factory.
 *
 * @author Weiran Liu
 * @date 2024/3/22
 */
public class PermutationNetworkFactory {
    /**
     * private constructor.
     */
    private PermutationNetworkFactory() {
        // empty
    }

    /**
     * Waksman network type.
     */
    public enum PermutationNetworkType {
        /**
         * Benes JDK
         */
        BENES_JDK,
        /**
         * Benes Native
         */
        BENES_NATIVE,
        /**
         * Waksman JDK
         */
        WAKSMAN_JDK,
        /**
         * Waksman Native
         */
        WAKSMAN_NATIVE,
    }

    /**
     * Creates a network.
     *
     * @param type           network type.
     * @param permutationMap permutation map.
     * @param <X>            input type.
     * @return a network.
     */
    public static <X> PermutationNetwork<X> createInstance(PermutationNetworkType type, int[] permutationMap) {
        switch (type) {
            case BENES_JDK:
                return BenesNetworkFactory.createInstance(BenesNetworkType.JDK, permutationMap);
            case BENES_NATIVE:
                return BenesNetworkFactory.createInstance(BenesNetworkType.NATIVE, permutationMap);
            case WAKSMAN_JDK:
                return WaksmanNetworkFactory.createInstance(WaksmanNetworkType.JDK, permutationMap);
            case WAKSMAN_NATIVE:
                return WaksmanNetworkFactory.createInstance(WaksmanNetworkType.NATIVE, permutationMap);
            default:
                throw new IllegalArgumentException("Invalid " + PermutationNetworkType.class.getSimpleName() + ": " + type.name());
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
    public static <X> PermutationNetwork<X> createInstance(PermutationNetworkType type, int n, byte[][] network) {
        switch (type) {
            case BENES_JDK:
                return BenesNetworkFactory.createInstance(BenesNetworkType.JDK, n, network);
            case BENES_NATIVE:
                return BenesNetworkFactory.createInstance(BenesNetworkType.NATIVE, n, network);
            case WAKSMAN_JDK:
                return WaksmanNetworkFactory.createInstance(WaksmanNetworkType.JDK, n, network);
            case WAKSMAN_NATIVE:
                return WaksmanNetworkFactory.createInstance(WaksmanNetworkType.NATIVE, n, network);
            default:
                throw new IllegalArgumentException("Invalid " + PermutationNetworkType.class.getSimpleName() + ": " + type.name());
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
    public static <X> PermutationNetwork<X> createInstance(EnvType envType, int[] permutationMap) {
        return WaksmanNetworkFactory.createInstance(envType, permutationMap);
    }
}
