package edu.alibaba.mpc4j.common.tool.benes;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 贝奈斯网络工厂类。
 *
 * @author Weiran Liu
 * @date 2021/12/25
 */
public class BenesNetworkFactory {
    /**
     * 私有构造函数
     */
    private BenesNetworkFactory() {
        // empty
    }

    /**
     * 贝奈斯网络类型。
     */
    public enum BenesNetworkType {
        /**
         * JDK贝奈斯网络
         */
        JDK_BENES_NETWORK,
        /**
         * 本地贝奈斯网络
         */
        NATIVE_BENES_NETWORK,
    }

    /**
     * 构建贝奈斯网络。
     *
     * @param benesNetworkType 贝奈斯网络类型。
     * @param permutationMap 置换表。
     * @param <X> 贝奈斯网络存储类型。
     * @return 贝奈斯网络。
     */
    public static <X> BenesNetwork<X> createInstance(BenesNetworkType benesNetworkType, int[] permutationMap) {
        switch (benesNetworkType) {
            case JDK_BENES_NETWORK:
                return new JdkBenesNetwork<>(permutationMap);
            case NATIVE_BENES_NETWORK:
                return new NativeBenesNetwork<>(permutationMap);
            default:
                throw new IllegalArgumentException("Invalid BenesNetworkType: " + benesNetworkType.name());
        }
    }

    /**
     * 构建贝奈斯网络。
     *
     * @param envType 环境类型。
     * @param permutationMap 置换表。
     * @param <X> 贝奈斯网络存储类型。
     * @return 贝奈斯网络。
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
                throw new IllegalArgumentException("Invalid EnvType: " + envType.name());
        }
    }
}
