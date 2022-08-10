package edu.alibaba.mpc4j.common.tool.okve.okvs;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.H2CuckooTableTcFinder;

import java.nio.ByteBuffer;

/**
 * 不经意键值存储器（OKVS）工厂类。
 *
 * @author Weiran Liu
 * @date 2021/09/05
 */
public class OkvsFactory {
    /**
     * OKVS类型。
     */
    public enum OkvsType {
        /**
         * 多项式插值
         */
        POLYNOMIAL,
        /**
         * MegaBin多项式插值
         */
        MEGA_BIN,
        /**
         * 乱码布隆过滤器
         */
        GBF,
        /**
         * 2哈希-单例乱码布谷鸟表
         */
        H2_SINGLETON_GCT,
        /**
         * 2哈希-两核乱码布谷鸟表
         */
        H2_TWO_CORE_GCT,
        /**
         * 2哈希-深度优先遍历乱码布谷鸟表
         */
        H2_DFS_GCT,
        /**
         * 3哈希-单例乱码布谷鸟表
         */
        H3_SINGLETON_GCT,
    }

    private OkvsFactory() {
        // empty
    }

    /**
     * 返回给定OKVS类型是否为二进制OKVS。
     *
     * @param okvsType 给定OKVS类型。
     * @return 此OKVS类型是否为二进制OKVS。
     */
    public static boolean isBinaryOkvsType(OkvsType okvsType) {
        switch (okvsType) {
            case POLYNOMIAL:
            case MEGA_BIN:
                return false;
            case GBF:
            case H2_DFS_GCT:
            case H2_TWO_CORE_GCT:
            case H2_SINGLETON_GCT:
            case H3_SINGLETON_GCT:
                return true;
            default:
                throw new IllegalArgumentException("Invalid OkvsType: " + okvsType.name());
        }
    }

    /**
     * 构建OKVS。
     *
     * @param envType  环境类型。
     * @param okvsType OKVS类型。
     * @param n        待编码的键值对数量。
     * @param l        输出比特长度，必须为Byte.SIZE的整数倍。
     * @param keys     哈希密钥。
     * @return OKVS。
     */
    public static Okvs<ByteBuffer> createInstance(EnvType envType, OkvsType okvsType, int n, int l, byte[][] keys) {
        assert keys.length == getHashNum(okvsType);
        switch (okvsType) {
            case POLYNOMIAL:
                return new PolynomialOkvs(envType, n, l);
            case MEGA_BIN:
                return new MegaBinOkvs(envType, n, l, keys[0]);
            case GBF:
                return new GbfBinaryOkvs<>(envType, n, l, keys);
            case H3_SINGLETON_GCT:
                return new H3TcGctBinaryOkvs<>(envType, n, l, keys);
            case H2_SINGLETON_GCT:
                return new H2TcGctBinaryOkvs<>(envType, n, l, keys, new CuckooTableSingletonTcFinder<>());
            case H2_TWO_CORE_GCT:
                return new H2TcGctBinaryOkvs<>(envType, n, l, keys, new H2CuckooTableTcFinder<>());
            case H2_DFS_GCT:
                return new H2DfsGctBinaryOkvs<>(envType, n, l, keys);
            default:
                throw new IllegalArgumentException("Invalid OkvsType：" + okvsType.name());
        }
    }

    /**
     * 构建二进制OKVS。
     *
     * @param envType  环境类型。
     * @param okvsType OKVS类型。
     * @param n        待编码的键值对数量。
     * @param l        输出比特长度，必须为Byte.SIZE的整数倍。
     * @param keys     哈希密钥。
     * @param <X>      键值类型。
     * @return 二进制OKVS。
     */
    public static <X> BinaryOkvs<X> createBinaryInstance(EnvType envType, OkvsType okvsType, int n, int l,
        byte[][] keys) {
        assert keys.length == getHashNum(okvsType);
        switch (okvsType) {
            case H3_SINGLETON_GCT:
                return new H3TcGctBinaryOkvs<>(envType, n, l, keys);
            case H2_SINGLETON_GCT:
                return new H2TcGctBinaryOkvs<>(envType, n, l, keys, new CuckooTableSingletonTcFinder<>());
            case H2_TWO_CORE_GCT:
                return new H2TcGctBinaryOkvs<>(envType, n, l, keys, new H2CuckooTableTcFinder<>());
            case H2_DFS_GCT:
                return new H2DfsGctBinaryOkvs<>(envType, n, l, keys);
            case GBF:
                return new GbfBinaryOkvs<>(envType, n, l, keys);
            default:
                throw new IllegalArgumentException("Invalid OkvsType：" + okvsType.name());
        }
    }

    /**
     * 返回OKVS的哈希函数数量。
     *
     * @param okvsType OKVS类型。
     * @return 哈希函数数量。
     */
    public static int getHashNum(OkvsType okvsType) {
        switch (okvsType) {
            case POLYNOMIAL:
                return 0;
            case MEGA_BIN:
                return 1;
            case H3_SINGLETON_GCT:
                return H3TcGctBinaryOkvs.HASH_NUM;
            case H2_SINGLETON_GCT:
            case H2_TWO_CORE_GCT:
                return H2TcGctBinaryOkvs.HASH_NUM;
            case H2_DFS_GCT:
                return H2DfsGctBinaryOkvs.HASH_NUM;
            case GBF:
                return GbfBinaryOkvs.HASH_NUM;
            default:
                throw new IllegalArgumentException("Invalid OkvsType" + okvsType.name());
        }
    }

    /**
     * 返回OKVS的长度m，m为Byte.SIZE的整数倍。
     *
     * @param okvsType OKVS类型。
     * @param n        待编码的键值对数量。
     * @return OKVS的长度m。
     */
    public static int getM(OkvsType okvsType, int n) {
        switch (okvsType) {
            case POLYNOMIAL:
                assert n > 1;
                return n;
            case MEGA_BIN:
                int binNum = MegaBinOkvs.getBinNum(n);
                int binSize = MegaBinOkvs.getBinSize(n);
                return binNum * binSize;
            case GBF:
                return GbfBinaryOkvs.getM(n);
            case H3_SINGLETON_GCT:
                return H3TcGctBinaryOkvs.getLm(n) + H3TcGctBinaryOkvs.getRm(n);
            case H2_SINGLETON_GCT:
            case H2_TWO_CORE_GCT:
                return H2TcGctBinaryOkvs.getLm(n) + H2TcGctBinaryOkvs.getRm(n);
            case H2_DFS_GCT:
                return H2DfsGctBinaryOkvs.getLm(n) + H2DfsGctBinaryOkvs.getRm(n);

            default:
                throw new IllegalArgumentException("Invalid OkvsType" + okvsType.name());
        }
    }
}
