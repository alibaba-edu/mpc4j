package edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.*;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;

/**
 * ECC-DOKVS factory.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
public class EccDokvsFactory {
    /**
     * ECC-DOKVS type.
     */
    public enum EccDokvsType {
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
     * @param ecc     ECC API.
     * @param n       number of key-value pairs.
     * @param keys    keys.
     * @return an instance.
     */
    public static <X> EccDokvs<X> createInstance(EnvType envType, EccDokvsType type, Ecc ecc, int n, byte[][] keys) {
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, getHashKeyNum(type));
        switch (type) {
            case DISTINCT_GBF:
                return new DistinctGbfEccDokvs<>(envType, ecc, n, keys[0]);
            case H2_TWO_CORE_GCT:
                return new H2TwoCoreGctEccDokvs<>(envType, ecc, n, keys);
            case H2_SINGLETON_GCT:
                return new H2SingletonGctEccDokvs<>(envType, ecc, n, keys);
            case H2_BLAZE_GCT:
                return new H2BlazeGctEccDokvs<>(envType, ecc, n, keys);
            case H2_NAIVE_CLUSTER_BLAZE_GCT:
                return new H2NaiveClusterBlazeGctEccOkvs<>(envType, ecc, n, keys);
            case H2_SPARSE_CLUSTER_BLAZE_GCT:
                return new H2SparseClusterBlazeGctEccDokvs<>(envType, ecc, n, keys);
            case H3_SINGLETON_GCT:
                return new H3SingletonGctEccDokvs<>(envType, ecc, n, keys);
            case H3_BLAZE_GCT:
                return new H3BlazeGctEccDokvs<>(envType, ecc, n, keys);
            case H3_NAIVE_CLUSTER_BLAZE_GCT:
                return new H3NaiveClusterBlazeGctEccDokvs<>(envType, ecc, n, keys);
            case H3_SPARSE_CLUSTER_BLAZE_GCT:
                return new H3SparseClusterBlazeGctDokvs<>(envType, ecc, n, keys);
            default:
                throw new IllegalArgumentException("Invalid " + EccDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Returns if the given type is a sparse type.
     *
     * @param type type.
     * @return true if the given type is a sparse type.
     */
    public static boolean isSparse(EccDokvsType type) {
        switch (type) {
            case DISTINCT_GBF:
            case H2_TWO_CORE_GCT:
            case H2_SINGLETON_GCT:
            case H2_BLAZE_GCT:
            case H2_SPARSE_CLUSTER_BLAZE_GCT:
            case H3_SINGLETON_GCT:
            case H3_BLAZE_GCT:
            case H3_SPARSE_CLUSTER_BLAZE_GCT:
                return true;
            case H2_NAIVE_CLUSTER_BLAZE_GCT:
            case H3_NAIVE_CLUSTER_BLAZE_GCT:
                return false;
            default:
                throw new IllegalArgumentException("Invalid " + EccDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a sparse instance.
     *
     * @param envType environment.
     * @param type    type.
     * @param ecc     ECC API.
     * @param n       number of key-value pairs.
     * @param keys    keys.
     * @return a sparse instance.
     */
    public static <X> SparseEccDokvs<X> createSparseInstance(EnvType envType, EccDokvsType type, Ecc ecc, int n, byte[][] keys) {
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, getHashKeyNum(type));
        switch (type) {
            case DISTINCT_GBF:
                return new DistinctGbfEccDokvs<>(envType, ecc, n, keys[0]);
            case H2_TWO_CORE_GCT:
                return new H2TwoCoreGctEccDokvs<>(envType, ecc, n, keys);
            case H2_SINGLETON_GCT:
                return new H2SingletonGctEccDokvs<>(envType, ecc, n, keys);
            case H2_BLAZE_GCT:
                return new H2BlazeGctEccDokvs<>(envType, ecc, n, keys);
            case H2_SPARSE_CLUSTER_BLAZE_GCT:
                return new H2SparseClusterBlazeGctEccDokvs<>(envType, ecc, n, keys);
            case H3_SINGLETON_GCT:
                return new H3SingletonGctEccDokvs<>(envType, ecc, n, keys);
            case H3_BLAZE_GCT:
                return new H3BlazeGctEccDokvs<>(envType, ecc, n, keys);
            case H3_SPARSE_CLUSTER_BLAZE_GCT:
                return new H3SparseClusterBlazeGctDokvs<>(envType, ecc, n, keys);
            default:
                throw new IllegalArgumentException("Invalid " + EccDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets number of required hash keys.
     *
     * @param type type.
     * @return number of required hash keys.
     */
    public static int getHashKeyNum(EccDokvsType type) {
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
                throw new IllegalArgumentException("Invalid " + EccDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets m.
     *
     * @param type    type.
     * @param n       number of key-value pairs.
     * @return m.
     */
    public static int getM(EccDokvsType type, int n) {
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
                throw new IllegalArgumentException("Invalid " + EccDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets the corresponding Zp-DOKVS for the given ECC-DOKVS.
     *
     * @param eccDokvsType ECC-DOKVS type.
     * @return corresponding Zp-DOKVS type.
     */
    public static ZpDokvsType getCorrespondingEccDokvsType(EccDokvsType eccDokvsType) {
        switch (eccDokvsType) {
            case DISTINCT_GBF:
                return ZpDokvsType.DISTINCT_GBF;
            case H2_TWO_CORE_GCT:
                return ZpDokvsType.H2_TWO_CORE_GCT;
            case H2_SINGLETON_GCT:
                return ZpDokvsType.H2_SINGLETON_GCT;
            case H2_BLAZE_GCT:
                return ZpDokvsType.H2_BLAZE_GCT;
            case H2_NAIVE_CLUSTER_BLAZE_GCT:
                return ZpDokvsType.H2_NAIVE_CLUSTER_BLAZE_GCT;
            case H2_SPARSE_CLUSTER_BLAZE_GCT:
                return ZpDokvsType.H2_SPARSE_CLUSTER_BLAZE_GCT;
            case H3_SINGLETON_GCT:
                return ZpDokvsType.H3_SINGLETON_GCT;
            case H3_BLAZE_GCT:
                return ZpDokvsType.H3_BLAZE_GCT;
            case H3_NAIVE_CLUSTER_BLAZE_GCT:
                return ZpDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT;
            case H3_SPARSE_CLUSTER_BLAZE_GCT:
                return ZpDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT;
            default:
                throw new IllegalArgumentException("Invalid " + EccDokvsType.class.getSimpleName() + ": " + eccDokvsType.name());
        }
    }
}
