package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.bc.*;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe.Ed25519CafeByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.bc.X25519BcByteMulElligatorEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe.RistrettoCafeByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.sodium.Ed25519SodiumByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.sodium.X25519SodiumByteMulEcc;

/**
 * 字节椭圆曲线工厂。
 *
 * @author Weiran Liu
 * @date 2022/9/1
 */
public class ByteEccFactory {
    /**
     * 私有构造函数。
     */
    private ByteEccFactory() {
        // empty
    }

    /**
     * 字节椭圆曲线枚举类
     */
    public enum ByteEccType {
        /**
         * Sodium实现的X25519
         */
        X25519_SODIUM,
        /**
         * BC实现的X25519
         */
        X25519_BC,
        /**
         * Sodium实现的ED25519
         */
        ED25519_SODIUM,
        /**
         * BC实现的ED25519
         */
        ED25519_BC,
        /**
         * Cafe实现的ED25519
         */
        ED25519_CAFE,
        /**
         * BC实现的Elligator编码X25519
         */
        X25519_ELLIGATOR_BC,
        /**
         * Cafe实现的Ristretto
         */
        RISTRETTO_CAFE,
    }

    /**
     * 创建全功能字节椭圆曲线。
     *
     * @param byteEccType 字节椭圆曲线类型。
     * @return 字节椭圆曲线。
     */
    public static ByteFullEcc createFullInstance(ByteEccType byteEccType) {
        switch (byteEccType) {
            case ED25519_SODIUM:
                return new Ed25519SodiumByteFullEcc();
            case ED25519_BC:
                return new Ed25519BcByteFullEcc();
            case ED25519_CAFE:
                return new Ed25519CafeByteFullEcc();
            case RISTRETTO_CAFE:
                return new RistrettoCafeByteFullEcc();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + ByteEccType.class.getSimpleName() + ": " + byteEccType.name()
                );
        }
    }

    /**
     * 创建字节椭圆曲线。
     *
     * @param envType 环境类型。
     * @return 字节椭圆曲线。
     */
    public static ByteFullEcc createFullInstance(EnvType envType) {
        switch (envType) {
            case STANDARD:
            case INLAND:
                return createFullInstance(ByteEccType.ED25519_SODIUM);
            case STANDARD_JDK:
            case INLAND_JDK:
                return createFullInstance(ByteEccType.ED25519_BC);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }

    /**
     * 创建乘法字节椭圆曲线。
     *
     * @param byteEccType 字节椭圆曲线类型。
     * @return 字节椭圆曲线。
     */
    public static ByteMulEcc createMulInstance(ByteEccType byteEccType) {
        switch (byteEccType) {
            case X25519_SODIUM:
                return new X25519SodiumByteMulEcc();
            case X25519_BC:
                return new X25519BcByteMulEcc();
            case ED25519_SODIUM:
                return new Ed25519SodiumByteFullEcc();
            case ED25519_BC:
                return new Ed25519BcByteFullEcc();
            case ED25519_CAFE:
                return new Ed25519CafeByteFullEcc();
            case RISTRETTO_CAFE:
                return new RistrettoCafeByteFullEcc();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + ByteEccType.class.getSimpleName() + ": " + byteEccType.name()
                );
        }
    }

    /**
     * 创建乘法字节椭圆曲线。
     *
     * @param envType 环境类型。
     * @return 乘法字节椭圆曲线。
     */
    public static ByteMulEcc createMulInstance(EnvType envType) {
        switch (envType) {
            case STANDARD:
            case INLAND:
                return createMulInstance(ByteEccType.X25519_SODIUM);
            case STANDARD_JDK:
            case INLAND_JDK:
                return createMulInstance(ByteEccType.X25519_BC);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }

    /**
     * 创建乘法字节椭圆曲线。
     *
     * @param byteEccType 字节椭圆曲线类型。
     * @return 字节椭圆曲线。
     */
    public static ByteMulElligatorEcc createMulElligatorInstance(ByteEccType byteEccType) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (byteEccType) {
            case X25519_ELLIGATOR_BC:
                return new X25519BcByteMulElligatorEcc();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + ByteEccType.class.getSimpleName() + ": " + byteEccType.name()
                );
        }
    }
}
