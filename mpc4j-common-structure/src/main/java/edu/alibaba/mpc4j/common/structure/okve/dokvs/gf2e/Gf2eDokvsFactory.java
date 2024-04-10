package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.*;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * GF(2^e)-DOKVS factory.
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
public class Gf2eDokvsFactory {
    /**
     * private constructor.
     */
    private Gf2eDokvsFactory() {
        // empty
    }

    /**
     * GF(2^l)-DOKVS type
     */
    public enum Gf2eDokvsType {
        /**
         * two-core garbled cuckoo table with 2 hash functions.
         */
        H2_TWO_CORE_GCT,
        /**
         * singleton garbled cuckoo table with 2 hash functions.
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
         * singleton garbled cuckoo table with 3 hash functions.
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
        /**
         * MegaBin
         */
        MEGA_BIN,
    }

    /**
     * Creates an instance.
     *
     * @param envType environment.
     * @param type    type.
     * @param n       number of key-value pairs.
     * @param l       value bit length.
     * @param keys    keys.
     * @return an instance.
     */
    public static <X> Gf2eDokvs<X> createInstance(EnvType envType, Gf2eDokvsType type, int n, int l, byte[][] keys) {
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, getHashKeyNum(type));
        switch (type) {
            case H2_TWO_CORE_GCT:
                return new H2TwoCoreGctGf2eDokvs<>(envType, n, l, keys);
            case H2_SINGLETON_GCT:
                return new H2SingletonGctGf2eDokvs<>(envType, n, l, keys);
            case H2_BLAZE_GCT:
                return new H2BlazeGctGf2eDokvs<>(envType, n, l, keys);
            case H2_NAIVE_CLUSTER_BLAZE_GCT:
                return new H2NaiveClusterBlazeGctGf2eDokvs<>(envType, n, l, keys);
            case H2_SPARSE_CLUSTER_BLAZE_GCT:
                return new H2SparseClusterBlazeGctGf2eDokvs<>(envType, n, l, keys);
            case H3_SINGLETON_GCT:
                return new H3SingletonGctGf2eDokvs<>(envType, n, l, keys);
            case H3_BLAZE_GCT:
                return new H3BlazeGctGf2eDokvs<>(envType, n, l, keys);
            case H3_NAIVE_CLUSTER_BLAZE_GCT:
                return new H3NaiveClusterBlazeGctGf2eDokvs<>(envType, n, l, keys);
            case H3_SPARSE_CLUSTER_BLAZE_GCT:
                return new H3SparseClusterBlazeGctGf2eDokvs<>(envType, n, l, keys);
            case DISTINCT_GBF:
                return new DistinctGbfGf2eDokvs<>(envType, n, l, keys[0]);
            case MEGA_BIN:
                return new MegaBinGf2eDokvs<>(envType, n, l, keys);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Returns if the given type is a binary type.
     *
     * @param type type.
     * @return true if the given type is a binary type.
     */
    public static boolean isBinary(Gf2eDokvsType type) {
        switch (type) {
            case H2_TWO_CORE_GCT:
            case H2_SINGLETON_GCT:
            case H2_BLAZE_GCT:
            case H2_NAIVE_CLUSTER_BLAZE_GCT:
            case H2_SPARSE_CLUSTER_BLAZE_GCT:
            case H3_SINGLETON_GCT:
            case H3_BLAZE_GCT:
            case H3_NAIVE_CLUSTER_BLAZE_GCT:
            case H3_SPARSE_CLUSTER_BLAZE_GCT:
            case DISTINCT_GBF:
                return true;
            case MEGA_BIN:
                return false;
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a binary instance.
     *
     * @param envType environment.
     * @param type    type.
     * @param n       number of key-value pairs.
     * @param l       value bit length.
     * @param keys    keys.
     * @return a binary instance.
     */
    public static <X> BinaryGf2eDokvs<X> createBinaryInstance(EnvType envType, Gf2eDokvsType type, int n, int l, byte[][] keys) {
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, getHashKeyNum(type));
        switch (type) {
            case H2_TWO_CORE_GCT:
                return new H2TwoCoreGctGf2eDokvs<>(envType, n, l, keys);
            case H2_SINGLETON_GCT:
                return new H2SingletonGctGf2eDokvs<>(envType, n, l, keys);
            case H2_BLAZE_GCT:
                return new H2BlazeGctGf2eDokvs<>(envType, n, l, keys);
            case H2_NAIVE_CLUSTER_BLAZE_GCT:
                return new H2NaiveClusterBlazeGctGf2eDokvs<>(envType, n, l, keys);
            case H2_SPARSE_CLUSTER_BLAZE_GCT:
                return new H2SparseClusterBlazeGctGf2eDokvs<>(envType, n, l, keys);
            case H3_SINGLETON_GCT:
                return new H3SingletonGctGf2eDokvs<>(envType, n, l, keys);
            case H3_BLAZE_GCT:
                return new H3BlazeGctGf2eDokvs<>(envType, n, l, keys);
            case H3_NAIVE_CLUSTER_BLAZE_GCT:
                return new H3NaiveClusterBlazeGctGf2eDokvs<>(envType, n, l, keys);
            case H3_SPARSE_CLUSTER_BLAZE_GCT:
                return new H3SparseClusterBlazeGctGf2eDokvs<>(envType, n, l, keys);
            case DISTINCT_GBF:
                return new DistinctGbfGf2eDokvs<>(envType, n, l, keys[0]);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Returns if the given type is a binary type.
     *
     * @param type type.
     * @return true if the given type is a binary type.
     */
    public static boolean isSparse(Gf2eDokvsType type) {
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
            case MEGA_BIN:
                return false;
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a sparse instance.
     *
     * @param envType environment.
     * @param type    type.
     * @param n       number of key-value pairs.
     * @param l       value bit length.
     * @param keys    keys.
     * @return a sparse instance.
     */
    public static <X> SparseGf2eDokvs<X> createSparseInstance(EnvType envType, Gf2eDokvsType type, int n, int l, byte[][] keys) {
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, getHashKeyNum(type));
        switch (type) {
            case H2_TWO_CORE_GCT:
                return new H2TwoCoreGctGf2eDokvs<>(envType, n, l, keys);
            case H2_SINGLETON_GCT:
                return new H2SingletonGctGf2eDokvs<>(envType, n, l, keys);
            case H2_BLAZE_GCT:
                return new H2BlazeGctGf2eDokvs<>(envType, n, l, keys);
            case H2_SPARSE_CLUSTER_BLAZE_GCT:
                return new H2SparseClusterBlazeGctGf2eDokvs<>(envType, n, l, keys);
            case H3_SINGLETON_GCT:
                return new H3SingletonGctGf2eDokvs<>(envType, n, l, keys);
            case H3_BLAZE_GCT:
                return new H3BlazeGctGf2eDokvs<>(envType, n, l, keys);
            case H3_SPARSE_CLUSTER_BLAZE_GCT:
                return new H3SparseClusterBlazeGctGf2eDokvs<>(envType, n, l, keys);
            case DISTINCT_GBF:
                return new DistinctGbfGf2eDokvs<>(envType, n, l, keys[0]);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets number of required hash keys.
     *
     * @param type type.
     * @return number of required hash keys.
     */
    public static int getHashKeyNum(Gf2eDokvsType type) {
        switch (type) {
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
            case DISTINCT_GBF:
                return DistinctGbfUtils.HASH_KEY_NUM;
            case MEGA_BIN:
                return MegaBinGf2eDokvs.HASH_KEY_NUM;
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets m.
     *
     * @param envType environment.
     * @param type type.
     * @param n    number of key-value pairs.
     * @return m.
     */
    public static int getM(EnvType envType, Gf2eDokvsType type, int n) {
        MathPreconditions.checkPositive("n", n);
        switch (type) {
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
            case DISTINCT_GBF:
                return DistinctGbfUtils.getM(n);
            case MEGA_BIN:
                return MegaBinGf2eDokvs.getM(envType, n);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }
}
