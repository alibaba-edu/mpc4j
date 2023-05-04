package edu.alibaba.mpc4j.common.tool.crypto.prg;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 伪随机数生成器工厂类。
 *
 * @author Weiran Liu
 * @date 2021/12/05
 */
public class PrgFactory {
    /**
     * 私有构造函数。
     */
    private PrgFactory() {
        // empty
    }

    /**
     * 伪随机数生成器类型
     */
    public enum PrgType {
        /**
         * JDK自带的SHA1PRNG伪随机数生成器
         */
        JDK_SECURE_RANDOM,
        /**
         * JDK的AES/ECB伪随机数生成器
         */
        JDK_AES_ECB,
        /**
         * JDK的AES/CTR伪随机数生成器
         */
        JDK_AES_CTR,
        /**
         * Bouncy Castle的SM4/ECB伪随机数生成器
         */
        BC_SM4_ECB,
        /**
         * Bouncy Castle的SM4/CTR伪随机数生成器
         */
        BC_SM4_CTR,
    }

    /**
     * 创建一个新的PRG实例。
     *
     * @param prgType PRG类型。
     * @param outputByteLength 输出字节长度。
     * @return PRG实例。
     */
    public static Prg createInstance(PrgType prgType, int outputByteLength) {
        assert outputByteLength > 0;
        switch (prgType) {
            case JDK_SECURE_RANDOM:
                return new JdkSecureRandomPrg(outputByteLength);
            case JDK_AES_CTR:
                return new JdkAesCtrPrg(outputByteLength);
            case JDK_AES_ECB:
                return new JdkAesEcbPrg(outputByteLength);
            case BC_SM4_CTR:
                return new BcSm4CtrPrg(outputByteLength);
            case BC_SM4_ECB:
                return new BcSm4EcbPrg(outputByteLength);
            default:
                throw new IllegalArgumentException("Invalid " + PrgType.class.getSimpleName() + ": " + prgType.name());
        }
    }

    /**
     * 创建一个新的PRG实例。
     *
     * @param envType 环境类型。
     * @param outputByteLength 输出字节长度。
     * @return PRG实例。
     */
    public static Prg createInstance(EnvType envType, int outputByteLength) {
        assert outputByteLength > 0;
        switch (envType) {
            case STANDARD:
            case STANDARD_JDK:
                return new JdkAesEcbPrg(outputByteLength);
            case INLAND:
            case INLAND_JDK:
                return new BcSm4EcbPrg(outputByteLength);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
