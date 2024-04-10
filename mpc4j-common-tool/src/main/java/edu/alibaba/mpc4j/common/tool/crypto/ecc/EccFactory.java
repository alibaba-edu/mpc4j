package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.bc.*;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.openssl.SecP256k1OpensslEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.openssl.SecP256r1OpensslEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.openssl.Sm2P256v1OpensslEcc;

/**
 * 椭圆曲线工厂类。
 *
 * @author Weiran Liu
 * @date 2021/12/12
 */
public class EccFactory {

    /**
     * 私有构造函数。
     */
    private EccFactory() {
        // empty
    }

    /**
     * 椭圆曲线枚举类
     */
    public enum EccType {
        /**
         * OpenSSL实现的SecP256k1
         */
        SEC_P256_K1_OPENSSL,
        /**
         * BC实现的SecP256k1
         */
        SEC_P256_K1_BC,
        /**
         * OpenSSL实现的SecP256r1
         */
        SEC_P256_R1_OPENSSL,
        /**
         * BC实现的SecP256r1
         */
        SEC_P256_R1_BC,
        /**
         * OpenSSL实现的sm2P256v1
         */
        SM2_P256_V1_OPENSSL,
        /**
         * BC实现的sm2P256v1
         */
        SM2_P256_V1_BC,
        /**
         * BC实现的Curve25519
         */
        CURVE25519_BC,
        /**
         * BC实现的ED25519
         */
        ED25519_BC,
    }

    /**
     * 创建椭圆曲线。
     *
     * @param eccType 椭圆曲线类型。
     * @return 椭圆曲线。
     */
    public static Ecc createInstance(EccType eccType) {
        switch (eccType) {
            case SEC_P256_K1_OPENSSL:
                return SecP256k1OpensslEcc.getInstance();
            case SEC_P256_K1_BC:
                return SecP256k1BcEcc.getInstance();
            case SEC_P256_R1_OPENSSL:
                return SecP256r1OpensslEcc.getInstance();
            case SEC_P256_R1_BC:
                return SecP256r1BcEcc.getInstance();
            case SM2_P256_V1_OPENSSL:
                return Sm2P256v1OpensslEcc.getInstance();
            case SM2_P256_V1_BC:
                return Sm2P256v1BcEcc.getInstance();
            case CURVE25519_BC:
                return Curve25519BcEcc.getInstance();
            case ED25519_BC:
                return Ed25519BcEcc.getInstance();
            default:
                throw new IllegalArgumentException("Invalid " + EccType.class.getSimpleName() + ": " + eccType.name());
        }
    }

    /**
     * 创建椭圆曲线。
     *
     * @param envType 环境类型。
     * @return 椭圆曲线。
     */
    public static Ecc createInstance(EnvType envType) {
        switch (envType) {
            case STANDARD:
                return createInstance(EccType.SEC_P256_R1_OPENSSL);
            case STANDARD_JDK:
                return createInstance(EccType.SEC_P256_K1_BC);
            case INLAND:
                return createInstance(EccType.SM2_P256_V1_OPENSSL);
            case INLAND_JDK:
                return createInstance(EccType.SM2_P256_V1_BC);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
