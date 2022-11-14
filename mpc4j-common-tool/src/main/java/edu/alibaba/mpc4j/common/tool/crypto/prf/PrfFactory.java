package edu.alibaba.mpc4j.common.tool.crypto.prf;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 伪随机函数工厂。
 *
 * @author Weiran Liu
 * @date 2021/12/08
 */
public class PrfFactory {
    /**
     * 私有构造函数
     */
    private PrfFactory() {
        // empty
    }

    public enum PrfType {
        /**
         * BC的SipHash
         */
        BC_SIP_HASH,
        /**
         * BC的SipHash128
         */
        BC_SIP128_HASH,
        /**
         * JDK的AES-CBC-MAC伪随机函数
         */
        JDK_AES_CBC,
        /**
         * BC的SM4-CBC-MAC伪随机函数
         */
        BC_SM4_CBC,
    }

    /**
     * 创建伪随机函数实例。
     *
     * @param prfType         伪随机函数类型。
     * @param outputByteLength 输出字节长度。
     * @return 伪随机函数实例。
     */
    public static Prf createInstance(PrfType prfType, int outputByteLength) {
        assert outputByteLength > 0;
        switch (prfType) {
            case BC_SIP_HASH:
                return new BcSipHashPrf(outputByteLength);
            case BC_SIP128_HASH:
                return new BcSip128HashPrf(outputByteLength);
            case JDK_AES_CBC:
                return new JdkAesCbcPrf(outputByteLength);
            case BC_SM4_CBC:
                return new BcSm4CbcPrf(outputByteLength);
            default:
                throw new IllegalArgumentException("Invalid " + PrfType.class.getSimpleName() + ": " + prfType.name());
        }
    }

    /**
     * 创建伪随机函数实例。经过测试，在任意输出字节长度下，JDK_AES_CBC的性能都是最优的。
     *
     * @param envType 环境类型。
     * @param outputByteLength 输出字节长度。
     * @return 伪随机函数实例。
     */
    public static Prf createInstance(EnvType envType, int outputByteLength) {
        switch (envType) {
            case STANDARD:
            case STANDARD_JDK:
                return createInstance(PrfType.JDK_AES_CBC, outputByteLength);
            case INLAND:
            case INLAND_JDK:
                return createInstance(PrfType.BC_SM4_CBC, outputByteLength);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
