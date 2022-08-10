package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.EnvType;

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
         * MCL实现的SecP256k1曲线
         */
        MCL_SEC_P256_K1,
        /**
         * BC实现的SecP256k1
         */
        BC_SEC_P256_K1,
        /**
         * MCL实现的SecP256r1曲线
         */
        MCL_SEC_P256_R1,
        /**
         * BC实现的SecP256r1
         */
        BC_SEC_P256_R1,
        /**
         * BC实现的Curve25519
         */
        BC_CURVE_25519,
        /**
         * BC实现的sm2P256v1
         */
        BC_SM2_P256_V1,
        /**
         * BC实现的ED25519
         */
        BC_ED_25519,
    }

    /**
     * 创建椭圆曲线。
     *
     * @param eccType 椭圆曲线类型。
     * @return 椭圆曲线。
     */
    public static Ecc createInstance(EccType eccType) {
        switch (eccType) {
            case BC_SEC_P256_K1:
                return new BcSecP256k1Ecc();
            case BC_SEC_P256_R1:
                return new BcSecP256r1Ecc();
            case BC_CURVE_25519:
                return new BcCurve25519Ecc();
            case BC_SM2_P256_V1:
                return new BcSm2P256v1Ecc();
            case MCL_SEC_P256_K1:
                return new MclSecP256k1Ecc();
            case MCL_SEC_P256_R1:
                return new MclSecP256r1Ecc();
            case BC_ED_25519:
                return new BcEd25519Ecc();
            default:
                throw new IllegalArgumentException("Invalid EccType:" + eccType.name());
        }
    }

    /**
     * 创建椭圆曲线。
     *
     * @param envType 椭圆曲线类型。
     * @return 椭圆曲线。
     */
    public static Ecc createInstance(EnvType envType) {
        switch (envType) {
            case STANDARD:
                return createInstance(EccType.MCL_SEC_P256_K1);
            case STANDARD_JDK:
                return createInstance(EccType.BC_SEC_P256_K1);
            case INLAND:
            case INLAND_JDK:
                return createInstance(EccType.BC_SM2_P256_V1);
            default:
                throw new IllegalArgumentException("Invalid EnvType:" + envType.name());
        }
    }
}
