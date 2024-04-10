package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.bc.*;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe.Ed25519CafeByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.bc.X25519BcByteMulElligatorEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe.RistrettoCafeByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.fourq.FourqByteFullEcc;
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
        /**
         * FourQ
         */
        FOUR_Q,
    }

    /**
     * Returns if the given ByteEccType is a ByteMulEcc.
     *
     * @param byteEccType the given ByteEccType.
     * @return true if it is a ByteMulEcc; false otherwise.
     */
    public static boolean isByteMulEcc(ByteEccType byteEccType) {
        switch (byteEccType) {
            case ED25519_SODIUM:
            case ED25519_BC:
            case ED25519_CAFE:
            case RISTRETTO_CAFE:
            case FOUR_Q:
            case X25519_SODIUM:
            case X25519_BC:
                return true;
            case X25519_ELLIGATOR_BC:
                return false;
            default:
                throw new IllegalArgumentException(
                    "Invalid " + ByteEccType.class.getSimpleName() + ": " + byteEccType.name()
                );
        }
    }

    /**
     * Returns if the given ByteEccType is a ByteFullEcc.
     *
     * @param byteEccType the given ByteEccType.
     * @return true if it is a ByteFullEcc; false otherwise.
     */
    public static boolean isByteFullEcc(ByteEccType byteEccType) {
        switch (byteEccType) {
            case ED25519_SODIUM:
            case ED25519_BC:
            case ED25519_CAFE:
            case RISTRETTO_CAFE:
            case FOUR_Q:
                return true;
            case X25519_SODIUM:
            case X25519_BC:
            case X25519_ELLIGATOR_BC:
                return false;
            default:
                throw new IllegalArgumentException(
                    "Invalid " + ByteEccType.class.getSimpleName() + ": " + byteEccType.name()
                );
        }
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
                return Ed25519SodiumByteFullEcc.getInstance();
            case ED25519_BC:
                return Ed25519BcByteFullEcc.getInstance();
            case ED25519_CAFE:
                return Ed25519CafeByteFullEcc.getInstance();
            case RISTRETTO_CAFE:
                return RistrettoCafeByteFullEcc.getInstance();
            case FOUR_Q:
                return FourqByteFullEcc.getInstance();
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
                return createFullInstance(ByteEccType.FOUR_Q);
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
                return X25519SodiumByteMulEcc.getInstance();
            case X25519_BC:
                return X25519BcByteMulEcc.getInstance();
            case ED25519_SODIUM:
                return Ed25519SodiumByteFullEcc.getInstance();
            case ED25519_BC:
                return Ed25519BcByteFullEcc.getInstance();
            case ED25519_CAFE:
                return Ed25519CafeByteFullEcc.getInstance();
            case RISTRETTO_CAFE:
                return RistrettoCafeByteFullEcc.getInstance();
            case FOUR_Q:
                return FourqByteFullEcc.getInstance();
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
                return X25519BcByteMulElligatorEcc.getInstance();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + ByteEccType.class.getSimpleName() + ": " + byteEccType.name()
                );
        }
    }
}
