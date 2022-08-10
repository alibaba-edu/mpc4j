package edu.alibaba.mpc4j.common.tool.okve.ovdm.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.H2CuckooTableTcFinder;

/**
 * GF(2^l)-OVDM工厂类。
 *
 * @author Weiran Liu
 * @date 2021/09/27
 */
public class Gf2eOvdmFactory {
    /**
     * 私有构造函数
     */
    private Gf2eOvdmFactory() {
        // empty
    }

    /**
     * GF(2^l)-OVDM类型。
     */
    public enum Gf2eOvdmType {
        /**
         * 2哈希-两核乱码布谷鸟表
         */
        H2_TWO_CORE_GCT,
        /**
         * 2哈希-单例乱码布谷鸟表
         */
        H2_SINGLETON_GCT,
        /**
         * 3哈希-单例乱码布谷鸟表
         */
        H3_SINGLETON_GCT,
    }

    /**
     * 构建GF(2^l)-OVDM。
     *
     * @param envType 环境类型。
     * @param type    GF(2^l)-OVDM类型。
     * @param n       待编码的键值对数量。
     * @param l       输出比特长度，必须为Byte.SIZE的整数倍。
     * @param keys    密钥。
     * @return GF(2 ^ l)-OVDM。
     */
    public static <X> Gf2eOvdm<X> createInstance(EnvType envType, Gf2eOvdmType type, int l, int n, byte[][] keys) {
        assert keys.length == getHashNum(type);
        switch (type) {
            case H3_SINGLETON_GCT:
                return new H3TcGctGf2eOvdm<>(envType, l, n, keys);
            case H2_SINGLETON_GCT:
                return new H2TcGctGf2eOvdm<>(envType, l, n, keys, new CuckooTableSingletonTcFinder<>());
            case H2_TWO_CORE_GCT:
                return new H2TcGctGf2eOvdm<>(envType, l, n, keys, new H2CuckooTableTcFinder<>());
            default:
                throw new IllegalArgumentException("Invalid Gf2xDvdmType: " + type.name());
        }
    }

    /**
     * 返回GF(2^l)-OVDM的哈希函数数量。
     *
     * @param type GF(2^l)-OVDM类型。
     * @return 哈希函数数量。
     */
    public static int getHashNum(Gf2eOvdmType type) {
        switch (type) {
            case H3_SINGLETON_GCT:
                return H3TcGctGf2eOvdm.HASH_NUM;
            case H2_SINGLETON_GCT:
            case H2_TWO_CORE_GCT:
                return H2TcGctGf2eOvdm.HASH_NUM;
            default:
                throw new IllegalArgumentException("Invalid Gf2eDvdmType: " + type.name());
        }
    }

    /**
     * 返回GF(2^l)-OVDM的长度m，m为Byte.SIZE的整数倍。
     *
     * @param type GF(2^l)-OVDM类型。
     * @param n    待编码的键值对数量。
     * @return GF(2 ^ l)-OVDM的长度m。
     */
    public static int getM(Gf2eOvdmType type, int n) {
        assert n > 0;
        switch (type) {
            case H3_SINGLETON_GCT:
                return H3TcGctGf2eOvdm.getLm(n) + H3TcGctGf2eOvdm.getRm(n);
            case H2_SINGLETON_GCT:
            case H2_TWO_CORE_GCT:
                return H2TcGctGf2eOvdm.getLm(n) + H2TcGctGf2eOvdm.getRm(n);
            default:
                throw new IllegalArgumentException("Invalid Gf2xDvdmType: " + type.name());
        }
    }
}
