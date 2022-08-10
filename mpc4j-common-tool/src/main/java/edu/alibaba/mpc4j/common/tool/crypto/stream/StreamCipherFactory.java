package edu.alibaba.mpc4j.common.tool.crypto.stream;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 流密码工厂类。
 *
 * @author Weiran Liu
 * @date 2022/8/9
 */
public class StreamCipherFactory {
    /**
     * 私有构造函数
     */
    private StreamCipherFactory() {
        // empty
    }

    /**
     * 流密码类型
     */
    public enum StreamCipherType {
        /**
         * JDK的AES-OFB模式
         */
        JDK_AES_OFB,
        /**
         * Bouncy Castle的AES-OFB模式
         */
        BC_AES_OFB,
        /**
         * Bouncy Castle的SM4-OFB模式
         */
        BC_SM4_OFB,
        /**
         * Bouncy Castle的ZUC-128
         */
        BC_ZUC_128,
    }

    /**
     * 创建流密码实例。
     *
     * @param type 类型。
     * @return 实例。
     */
    public static StreamCipher createInstance(StreamCipherType type) {
        switch (type) {
            case JDK_AES_OFB:
                return new JdkAesOfbStreamCipher();
            case BC_AES_OFB:
                return new BcAesOfbStreamCipher();
            case BC_SM4_OFB:
                return new BcSm4OfbStreamCipher();
            case BC_ZUC_128:
                return new BcZuc128StreamCipher();
            default:
                throw new IllegalArgumentException("Invalid " + StreamCipherType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * 创建流密码实例。
     *
     * @param envType 环境类型。
     * @return 实例。
     */
    public static StreamCipher createInstance(EnvType envType) {
        switch (envType) {
            case STANDARD:
            case STANDARD_JDK:
                return new BcAesOfbStreamCipher();
            case INLAND:
            case INLAND_JDK:
                return new BcSm4OfbStreamCipher();
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType);
        }
    }
}
