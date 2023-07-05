package edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;

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
         * singleton garbled cuckoo table with 3 hash functions.
         */
        H3_SINGLETON_GCT,
        /**
         * blazing fast using garbled cuckoo table with 3 hash function.
         */
        H3_BLAZE_GCT,
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
     * @param n       number of key-value pairs.
     * @param l       value bit length.
     * @param keys    keys.
     * @return and instance.
     */
    public static <X> Gf2eDokvs<X> createInstance(EnvType envType, Gf2eDokvsType type, int n, int l, byte[][] keys) {
        switch (type) {
            case H2_TWO_CORE_GCT:
                return new H2TwoCoreGctGf2eDokvs<>(envType, n, l, keys);
            case H2_SINGLETON_GCT:
                return new H2SingletonGctGf2eDokvs<>(envType, n, l, keys);
            case H2_BLAZE_GCT:
                return new H2BlazeGctGf2eDokvs<>(envType, n, l, keys);
            case H3_SINGLETON_GCT:
                return new H3SingletonGctGfe2Dokvs<>(envType, n, l, keys);
            case H3_BLAZE_GCT:
                return new H3BlazeGctGf2eDokvs<>(envType, n, l, keys);
            case DISTINCT_GBF:
                return new DistinctGbfDokvs<>(envType, n, l, keys);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets number of required hashes.
     *
     * @param type type.
     * @return number of required hashes.
     */
    public static int getHashNum(Gf2eDokvsType type) {
        switch (type) {
            case H2_TWO_CORE_GCT:
            case H2_SINGLETON_GCT:
            case H2_BLAZE_GCT:
                return AbstractH2GctGf2eDokvs.TOTAL_HASH_NUM;
            case H3_SINGLETON_GCT:
            case H3_BLAZE_GCT:
                return H3SingletonGctGfe2Dokvs.TOTAL_HASH_NUM;
            case DISTINCT_GBF:
                return DistinctGbfDokvs.TOTAL_HASH_NUM;
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets m with m % Byte.SIZE == 0.
     *
     * @param type type.
     * @param n    number of key-value pairs.
     * @return m.
     */
    public static int getM(Gf2eDokvsType type, int n) {
        assert n > 0;
        switch (type) {
            case H2_TWO_CORE_GCT:
            case H2_SINGLETON_GCT:
                return H2TwoCoreGctGf2eDokvs.getLm(n) + H2TwoCoreGctGf2eDokvs.getRm(n);
            case H2_BLAZE_GCT:
                return H2BlazeGctGf2eDokvs.getLm(n) + H2BlazeGctGf2eDokvs.getRm(n);
            case H3_SINGLETON_GCT:
                return H3SingletonGctGfe2Dokvs.getLm(n) + H3SingletonGctGfe2Dokvs.getRm(n);
            case H3_BLAZE_GCT:
                return H3BlazeGctGf2eDokvs.getLm(n) + H3BlazeGctGf2eDokvs.getRm(n);
            case DISTINCT_GBF:
                return DistinctGbfDokvs.getM(n);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }
}
