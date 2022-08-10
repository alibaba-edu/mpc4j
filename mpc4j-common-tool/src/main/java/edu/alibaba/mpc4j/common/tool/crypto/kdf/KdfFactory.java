package edu.alibaba.mpc4j.common.tool.crypto.kdf;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 密钥派生函数工厂类。
 *
 * @author Weiran Liu
 * @date 2021/12/31
 */
public class KdfFactory {

    /**
     * 私有构造函数
     */
    private KdfFactory() {
        // empty
    }

    /**
     * 密钥派生协议类型
     */
    public enum KdfType {
        /**
         * JDK的SHA256
         */
        JDK_SHA256,
        /**
         * 本地的SHA256
         */
        NATIVE_SHA256,
        /**
         * Bouncy Castle的SM3
         */
        BC_SM3,
        /**
         * Bouncy Castle的Blake2b
         */
        BC_BLAKE_2B,
        /**
         * 本地Blake2b
         */
        NATIVE_BLAKE_2B,
        /**
         * 本地Blake3
         */
        NATIVE_BLAKE_3,
    }

    /**
     * 创建密钥派生函数实例。
     *
     * @param kdfType 密钥派生函数类型。
     * @return 密钥派生函数实例。
     */
    public static Kdf createInstance(KdfType kdfType) {
        switch (kdfType) {
            case JDK_SHA256:
                return new JdkSha256Kdf();
            case NATIVE_SHA256:
                return new NativeSha256Kdf();
            case BC_SM3:
                return new BcSm3Kdf();
            case BC_BLAKE_2B:
                return new BcBlake2bKdf();
            case NATIVE_BLAKE_2B:
                return new NativeBlake2bKdf();
            case NATIVE_BLAKE_3:
                return new NativeBlake3Kdf();
            default:
                throw new IllegalArgumentException("Invalid KdfType " + kdfType.name());
        }
    }

    /**
     * 创建密钥派生函数实例。
     *
     * @param envType 环境类型。
     * @return 密钥派生函数实例。
     */
    public static Kdf createInstance(EnvType envType) {
        switch (envType) {
            case STANDARD:
            case STANDARD_JDK:
                return new JdkSha256Kdf();
            case INLAND:
            case INLAND_JDK:
                return new BcSm3Kdf();
            default:
                throw new IllegalArgumentException("Invalid EnvType " + envType.name());
        }
    }
}
