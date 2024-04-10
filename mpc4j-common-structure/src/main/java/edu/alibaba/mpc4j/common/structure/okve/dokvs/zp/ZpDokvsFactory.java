package edu.alibaba.mpc4j.common.structure.okve.dokvs.zp;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.*;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.math.BigInteger;

/**
 * Zp-DOKVS factory.
 *
 * @author Weiran Liu
 * @date 2024/2/19
 */
public class ZpDokvsFactory {
    /**
     * Zp-DOKVS type.
     */
    public enum ZpDokvsType {
        /**
         * 2-hash two-core Garbled Cuckoo Table
         */
        H2_TWO_CORE_GCT,
        /**
         * 2-hash singleton Garbled Cuckoo Table
         */
        H2_SINGLETON_GCT,
        /**
         * blazing fast using garbled cuckoo table with 2 hash function.
         */
        H2_BLAZE_GCT,
        /**
         * cluster blazing fast using garbled cuckoo table with 2 hash function.
         */
        H2_NAIVE_CLUSTER_BLAZE_GCT,
        /**
         * sparse cluster blazing fast using garbled cuckoo table with 2 hash function.
         */
        H2_SPARSE_CLUSTER_BLAZE_GCT,
        /**
         * 3-hash singleton Garbled Cuckoo Table
         */
        H3_SINGLETON_GCT,
        /**
         * blazing fast using garbled cuckoo table with 3 hash function.
         */
        H3_BLAZE_GCT,
        /**
         * cluster blazing fast using garbled cuckoo table with 3 hash function.
         */
        H3_NAIVE_CLUSTER_BLAZE_GCT,
        /**
         * sparse cluster blazing fast using garbled cuckoo table with 3 hash function.
         */
        H3_SPARSE_CLUSTER_BLAZE_GCT,
        /**
         * distinct garbled bloom filter
         */
        DISTINCT_GBF,
    }

    /**
     * Creates an instance.
     *
     * @param envType environment.
     * @param type    type.
     * @param p       prime.
     * @param n       number of key-value pairs.
     * @param keys    keys.
     * @return an instance.
     */
    public static <X> ZpDokvs<X> createInstance(EnvType envType, ZpDokvsType type, BigInteger p, int n, byte[][] keys) {
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, getHashKeyNum(type));
        switch (type) {
            case DISTINCT_GBF:
                return new DistinctGbfZpDokvs<>(envType, p, n, keys[0]);
            case H2_TWO_CORE_GCT:
                return new H2TwoCoreGctZpDokvs<>(envType, p, n, keys);
            case H2_SINGLETON_GCT:
                return new H2SingletonGctZpDokvs<>(envType, p, n, keys);
            case H2_BLAZE_GCT:
                return new H2BlazeGctZpDokvs<>(envType, p, n, keys);
            case H2_NAIVE_CLUSTER_BLAZE_GCT:
                return new H2NaiveClusterBlazeGctZpDokvs<>(envType, p, n, keys);
            case H2_SPARSE_CLUSTER_BLAZE_GCT:
                return new H2SparseClusterBlazeGctZpDokvs<>(envType, p, n, keys);
            case H3_SINGLETON_GCT:
                return new H3SingletonGctZpDokvs<>(envType, p, n, keys);
            case H3_BLAZE_GCT:
                return new H3BlazeGctZpDokvs<>(envType, p, n, keys);
            case H3_NAIVE_CLUSTER_BLAZE_GCT:
                return new H3NaiveClusterBlazeGctZpDokvs<>(envType, p, n, keys);
            case H3_SPARSE_CLUSTER_BLAZE_GCT:
                return new H3SparseClusterBlazeGctZpDokvs<>(envType, p, n, keys);
            default:
                throw new IllegalArgumentException("Invalid " + ZpDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Returns if the given type is a sparse type.
     *
     * @param type type.
     * @return true if the given type is a sparse type.
     */
    public static boolean isSparse(ZpDokvsType type) {
        switch (type) {
            case H2_TWO_CORE_GCT:
            case H2_SINGLETON_GCT:
            case H2_BLAZE_GCT:
            case H2_SPARSE_CLUSTER_BLAZE_GCT:
            case H3_SINGLETON_GCT:
            case H3_BLAZE_GCT:
            case H3_SPARSE_CLUSTER_BLAZE_GCT:
            case DISTINCT_GBF:
                return true;
            case H2_NAIVE_CLUSTER_BLAZE_GCT:
            case H3_NAIVE_CLUSTER_BLAZE_GCT:
                return false;
            default:
                throw new IllegalArgumentException("Invalid " + ZpDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a sparse instance.
     *
     * @param envType environment.
     * @param type    type.
     * @param p       prime.
     * @param n       number of key-value pairs.
     * @param keys    keys.
     * @return a sparse instance.
     */
    public static <X> SparseZpDokvs<X> createSparseInstance(EnvType envType, ZpDokvsType type, BigInteger p, int n, byte[][] keys) {
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, getHashKeyNum(type));
        switch (type) {
            case DISTINCT_GBF:
                return new DistinctGbfZpDokvs<>(envType, p, n, keys[0]);
            case H2_TWO_CORE_GCT:
                return new H2TwoCoreGctZpDokvs<>(envType, p, n, keys);
            case H2_SINGLETON_GCT:
                return new H2SingletonGctZpDokvs<>(envType, p, n, keys);
            case H2_BLAZE_GCT:
                return new H2BlazeGctZpDokvs<>(envType, p, n, keys);
            case H2_SPARSE_CLUSTER_BLAZE_GCT:
                return new H2SparseClusterBlazeGctZpDokvs<>(envType, p, n, keys);
            case H3_SINGLETON_GCT:
                return new H3SingletonGctZpDokvs<>(envType, p, n, keys);
            case H3_BLAZE_GCT:
                return new H3BlazeGctZpDokvs<>(envType, p, n, keys);
            case H3_SPARSE_CLUSTER_BLAZE_GCT:
                return new H3SparseClusterBlazeGctZpDokvs<>(envType, p, n, keys);
            default:
                throw new IllegalArgumentException("Invalid " + ZpDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets number of required hash keys.
     *
     * @param type type.
     * @return number of required hash keys.
     */
    public static int getHashKeyNum(ZpDokvsType type) {
        switch (type) {
            case DISTINCT_GBF:
                return DistinctGbfUtils.HASH_KEY_NUM;
            case H2_TWO_CORE_GCT:
            case H2_SINGLETON_GCT:
            case H2_BLAZE_GCT:
                return H2GctDokvsUtils.HASH_KEY_NUM;
            case H2_NAIVE_CLUSTER_BLAZE_GCT:
            case H2_SPARSE_CLUSTER_BLAZE_GCT:
                return H2ClusterBlazeGctDokvsUtils.HASH_KEY_NUM;
            case H3_SINGLETON_GCT:
            case H3_BLAZE_GCT:
                return H3GctDokvsUtils.HASH_KEY_NUM;
            case H3_NAIVE_CLUSTER_BLAZE_GCT:
            case H3_SPARSE_CLUSTER_BLAZE_GCT:
                return H3ClusterBlazeGctDokvsUtils.HASH_KEY_NUM;
            default:
                throw new IllegalArgumentException("Invalid " + ZpDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets m.
     *
     * @param type    type.
     * @param n       number of key-value pairs.
     * @return m.
     */
    public static int getM(ZpDokvsType type, int n) {
        MathPreconditions.checkPositive("n", n);
        switch (type) {
            case DISTINCT_GBF:
                return DistinctGbfUtils.getM(n);
            case H2_TWO_CORE_GCT:
            case H2_SINGLETON_GCT:
                return H2NaiveGctDokvsUtils.getLm(n) + H2NaiveGctDokvsUtils.getRm(n);
            case H2_BLAZE_GCT:
                return H2BlazeGctDokvsUtils.getLm(n) + H2BlazeGctDokvsUtils.getRm(n);
            case H2_NAIVE_CLUSTER_BLAZE_GCT:
            case H2_SPARSE_CLUSTER_BLAZE_GCT:
                return H2ClusterBlazeGctDokvsUtils.getM(n);
            case H3_SINGLETON_GCT:
                return H3NaiveGctDokvsUtils.getLm(n) + H3NaiveGctDokvsUtils.getRm(n);
            case H3_BLAZE_GCT:
                return H3BlazeGctDovsUtils.getLm(n) + H3BlazeGctDovsUtils.getRm(n);
            case H3_NAIVE_CLUSTER_BLAZE_GCT:
            case H3_SPARSE_CLUSTER_BLAZE_GCT:
                return H3ClusterBlazeGctDokvsUtils.getM(n);
            default:
                throw new IllegalArgumentException("Invalid " + ZpDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }
}
