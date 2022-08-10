package edu.alibaba.mpc4j.common.tool.okve.ovdm.zp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.H2CuckooTableTcFinder;

import java.math.BigInteger;

/**
 * Zp-OVDM工厂。
 *
 * @author Weiran Liu
 * @date 2021/10/01
 */
public class ZpOvdmFactory {
    /**
     * 私有构造函数
     */
    private ZpOvdmFactory() {
        // empty
    }

    /**
     * Zp-OVDM类型。
     */
    public enum ZpOvdmType {
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
     * 构建Zp-OVDM。
     *
     * @param envType 环境类型。
     * @param type    Zp-OVDM类型。
     * @param p       模数p。
     * @param n       待编码的键值对数量。
     * @param keys    哈希密钥。
     * @return Zp-OVDM。
     */
    public static <X> ZpOvdm<X> createInstance(EnvType envType, ZpOvdmType type, BigInteger p, int n, byte[][] keys) {
        assert keys.length == getHashNum(type);
        switch (type) {
            case H3_SINGLETON_GCT:
                return new H3TcGctZpOvdm<>(envType, p, n, keys);
            case H2_SINGLETON_GCT:
                return new H2TcGctZpOvdm<>(envType, p, n, keys, new CuckooTableSingletonTcFinder<>());
            case H2_TWO_CORE_GCT:
                return new H2TcGctZpOvdm<>(envType, p, n, keys, new H2CuckooTableTcFinder<>());
            default:
                throw new IllegalArgumentException("Invalid ZpOvdmType: " + type.name());
        }
    }

    /**
     * 返回Zp-OVDM的哈希函数数量。
     *
     * @param zpOvdmType Zp-OVDM类型。
     * @return 哈希函数数量。
     */
    public static int getHashNum(ZpOvdmType zpOvdmType) {
        switch (zpOvdmType) {
            case H3_SINGLETON_GCT:
                return H3TcGctZpOvdm.HASH_NUM;
            case H2_SINGLETON_GCT:
            case H2_TWO_CORE_GCT:
                return H2TcGctZpOvdm.HASH_NUM;
            default:
                throw new IllegalArgumentException("Invalid ZpOvdmType: " + zpOvdmType.name());
        }
    }

    /**
     * 返回Zp-OVDM的长度m，m为Byte.SIZE的整数倍。
     *
     * @param zpOvdmType Zp-OVDM类型。
     * @param n          待编码的键值对数量。
     * @return Zp-OVDM的长度m。
     */
    public static int getM(ZpOvdmType zpOvdmType, int n) {
        Preconditions.checkArgument(n > 0);
        switch (zpOvdmType) {
            case H3_SINGLETON_GCT:
                return H3TcGctZpOvdm.getLm(n) + H3TcGctZpOvdm.getRm(n);
            case H2_SINGLETON_GCT:
            case H2_TWO_CORE_GCT:
                return H2TcGctZpOvdm.getLm(n) + H2TcGctZpOvdm.getRm(n);
            default:
                throw new IllegalArgumentException("Invalid ZpOvdmType: " + zpOvdmType.name());
        }
    }
}
