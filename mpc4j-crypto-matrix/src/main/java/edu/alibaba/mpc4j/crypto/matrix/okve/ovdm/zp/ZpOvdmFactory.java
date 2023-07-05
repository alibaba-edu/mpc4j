package edu.alibaba.mpc4j.crypto.matrix.okve.ovdm.zp;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.crypto.matrix.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.crypto.matrix.okve.cuckootable.H2CuckooTableTcFinder;

import java.math.BigInteger;

/**
 * Zp-OVDM factory.
 *
 * @author Weiran Liu
 * @date 2021/10/01
 */
public class ZpOvdmFactory {
    /**
     * private constructor.
     */
    private ZpOvdmFactory() {
        // empty
    }

    /**
     * Zp-OVDM type.
     */
    public enum ZpOvdmType {
        /**
         * 2-hash two-core Garbled Cuckoo Table
         */
        H2_TWO_CORE_GCT,
        /**
         * 2-hash singleton Garbled Cuckoo Table
         */
        H2_SINGLETON_GCT,
        /**
         * 3-hash singleton Garbled Cuckoo Table
         */
        H3_SINGLETON_GCT,
        /**
         * LPRST21 Garbled Bloom Filter
         */
        LPRST21_GBF,
    }

    /**
     * Creates an empty Zp-OVDM.
     *
     * @param envType environment.
     * @param type    Zp-OVDM type.
     * @param p       p.
     * @param n       number of key-value pairs.
     * @param keys    hash keys.
     * @return an empty Zp-OVDM.
     */
    public static <X> ZpOvdm<X> createInstance(EnvType envType, ZpOvdmType type, BigInteger p, int n, byte[][] keys) {
        MathPreconditions.checkEqual("keys.length", "hashNum", keys.length, getHashNum(type));
        switch (type) {
            case H3_SINGLETON_GCT:
                return new H3TcGctZpOvdm<>(envType, p, n, keys);
            case H2_SINGLETON_GCT:
                return new H2TcGctZpOvdm<>(envType, p, n, keys, new CuckooTableSingletonTcFinder<>());
            case H2_TWO_CORE_GCT:
                return new H2TcGctZpOvdm<>(envType, p, n, keys, new H2CuckooTableTcFinder<>());
            case LPRST21_GBF:
                return new Lprst21GbfZpOvdm<>(envType, p, n, keys);
            default:
                throw new IllegalArgumentException("Invalid " + ZpOvdmType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets hash num.
     *
     * @param type Zp-OVDM type.
     * @return hash num.
     */
    public static int getHashNum(ZpOvdmType type) {
        switch (type) {
            case H3_SINGLETON_GCT:
                return H3TcGctZpOvdm.HASH_NUM;
            case H2_SINGLETON_GCT:
            case H2_TWO_CORE_GCT:
                return H2TcGctZpOvdm.HASH_NUM;
            case LPRST21_GBF:
                return Lprst21GbfZpOvdm.HASH_NUM;
            default:
                throw new IllegalArgumentException("Invalid " + ZpOvdmType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets m, with m % Byte.SIZE == 0.
     *
     * @param type Zp-OVDM type.
     * @param n    number of key-value pairs.
     * @return m.
     */
    public static int getM(ZpOvdmType type, int n) {
        MathPreconditions.checkPositive("n", n);
        switch (type) {
            case H3_SINGLETON_GCT:
                return H3TcGctZpOvdm.getLm(n) + H3TcGctZpOvdm.getRm(n);
            case H2_SINGLETON_GCT:
            case H2_TWO_CORE_GCT:
                return H2TcGctZpOvdm.getLm(n) + H2TcGctZpOvdm.getRm(n);
            case LPRST21_GBF:
                return Lprst21GbfZpOvdm.getM(n);
            default:
                throw new IllegalArgumentException("Invalid " + ZpOvdmType.class.getSimpleName() + ": " + type.name());
        }
    }
}
