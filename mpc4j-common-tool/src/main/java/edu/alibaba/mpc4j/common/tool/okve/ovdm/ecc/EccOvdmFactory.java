package edu.alibaba.mpc4j.common.tool.okve.ovdm.ecc;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.H2CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.zp.ZpOvdmFactory.ZpOvdmType;

/**
 * 椭圆曲线不经意映射值解密匹配（OVDM）工厂类。
 *
 * @author Weiran Liu
 * @date 2021/09/07
 */
public class EccOvdmFactory {
    /**
     * 椭圆曲线OVDM类型。
     */
    public enum EccOvdmType {
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

    private EccOvdmFactory() {
        // empty
    }

    /**
     * 构建OVDM。
     *
     * @param envType     环境类型。
     * @param eccOvdmType OVDM类型。
     * @param ecc         椭圆曲线操作接口。
     * @param n           待编码的键值对数量。
     * @param keys        哈希密钥。
     * @return 椭圆曲线OVDM。
     */
    public static <X> EccOvdm<X> createInstance(EnvType envType, EccOvdmType eccOvdmType, Ecc ecc, int n,
        byte[][] keys) {
        assert keys.length == getHashNum(eccOvdmType);
        switch (eccOvdmType) {
            case H3_SINGLETON_GCT:
                return new H3TcGctEccOvdm<>(envType, ecc, n, keys);
            case H2_SINGLETON_GCT:
                return new H2TcGctEccOvdm<>(envType, ecc, n, keys, new CuckooTableSingletonTcFinder<>());
            case H2_TWO_CORE_GCT:
                return new H2TcGctEccOvdm<>(envType, ecc, n, keys, new H2CuckooTableTcFinder<>());
            default:
                throw new IllegalArgumentException("Invalid EccOvdmType: " + eccOvdmType.name());
        }
    }

    /**
     * 返回椭圆曲线OVDM的哈希函数数量。
     *
     * @param eccOvdmType 椭圆曲线OVDM类型。
     * @return 哈希函数数量。
     */
    public static int getHashNum(EccOvdmType eccOvdmType) {
        switch (eccOvdmType) {
            case H3_SINGLETON_GCT:
                return H3TcGctEccOvdm.HASH_NUM;
            case H2_SINGLETON_GCT:
            case H2_TWO_CORE_GCT:
                return H2TcGctEccOvdm.HASH_NUM;
            default:
                throw new IllegalArgumentException("Invalid EccOvdmType: " + eccOvdmType.name());
        }
    }

    /**
     * 返回椭圆曲线OVDM的长度m，m为Byte.SIZE的整数倍。
     *
     * @param eccOvdmType 椭圆曲线OVDM类型。
     * @param n           待编码的键值对数量。
     * @return OVDM的长度m。
     */
    public static int getM(EccOvdmType eccOvdmType, int n) {
        assert n > 0;
        switch (eccOvdmType) {
            case H3_SINGLETON_GCT:
                return H3TcGctEccOvdm.getLm(n) + H3TcGctEccOvdm.getRm(n);
            case H2_SINGLETON_GCT:
            case H2_TWO_CORE_GCT:
                return H2TcGctEccOvdm.getLm(n) + H2TcGctEccOvdm.getRm(n);
            default:
                throw new IllegalArgumentException("Invalid EccOvdmType: " + eccOvdmType.name());
        }
    }

    /**
     * 返回椭圆曲线OVDM类型对应的Zp-OVDM类型。
     *
     * @param eccOvdmType 椭圆曲线OVDM类型。
     * @return 对应的Zp-OVDM类型。
     */
    public static ZpOvdmType getRelatedEccOvdmType(EccOvdmType eccOvdmType) {
        switch (eccOvdmType) {
            case H2_TWO_CORE_GCT:
                return ZpOvdmType.H2_TWO_CORE_GCT;
            case H2_SINGLETON_GCT:
                return ZpOvdmType.H2_SINGLETON_GCT;
            case H3_SINGLETON_GCT:
                return ZpOvdmType.H3_SINGLETON_GCT;
            default:
                throw new IllegalArgumentException("Invalid EccOvdmType: " + eccOvdmType.name());
        }
    }
}
