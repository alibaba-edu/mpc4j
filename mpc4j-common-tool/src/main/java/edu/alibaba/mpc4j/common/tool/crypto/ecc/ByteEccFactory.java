package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.bc.*;

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
         * BC实现的X25519
         */
        X25519_BC,
        /**
         * BC实现的ED25519
         */
        ED25519_BC,
    }

    /**
     * 创建全功能字节椭圆曲线。
     *
     * @param byteEccType 字节椭圆曲线类型。
     * @return 字节椭圆曲线。
     */
    public static ByteFullEcc createFullInstance(ByteEccType byteEccType) {
        switch (byteEccType) {
            case ED25519_BC:
                return new Ed25519BcByteFullEcc();
            case X25519_BC:
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
            case STANDARD_JDK:
            case INLAND:
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
            case ED25519_BC:
                return new Ed25519BcByteFullEcc();
            case X25519_BC:
                return new X25519BcByteMulEcc();
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
            case STANDARD_JDK:
            case INLAND:
            case INLAND_JDK:
                return createFullInstance(ByteEccType.X25519_BC);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
