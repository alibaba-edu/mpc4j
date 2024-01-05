package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k;

import com.google.common.collect.ImmutableMap;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;

import java.util.Map;

/**
 * GF(2^k)-DOKVS factory.
 *
 * @author Weiran Liu
 * @date 2023/7/11
 */
public class Gf2kDokvsFactory {
    /**
     * private constructor.
     */
    private Gf2kDokvsFactory() {
        // empty
    }

    /**
     * GF(2^k)-DOKVS type
     */
    public enum Gf2kDokvsType {
        /**
         * binary singleton garbled cuckoo table with 2 hash functions.
         */
        H2_BINARY_SINGLETON_GCT,
        /**
         * binary blazing fast garbled cuckoo table with 2 hash functions.
         */
        H2_BINARY_BLAZE_GCT,
        /**
         * binary singleton garbled cuckoo table with 3 hash functions.
         */
        H3_BINARY_SINGLETON_GCT,
        /**
         * binary blazing fast garbled cuckoo table with 3 hash function.
         */
        H3_BINARY_BLAZE_GCT,
        /**
         * field blazing fast using garbled cuckoo table with 3 hash function.
         */
        H3_FIELD_BLAZE_GCT,
        /**
         * binary cluster blazing fast garbled cuckoo table with 3 hash functions.
         */
        H3_CLUSTER_BINARY_BLAZE_GCT,
        /**
         * field cluster blazing fast garbled cuckoo table with 3 hash functions.
         */
        H3_CLUSTER_FIELD_BLAZE_GCT,
        /**
         * MegaBin
         */
        MEGA_BIN,
    }

    /**
     * GF2K -> GF2E map
     */
    private static final Map<Gf2kDokvsType, Gf2eDokvsType> BINARY_GF2K_GF2E_TYPE_MAP = ImmutableMap.<Gf2kDokvsType, Gf2eDokvsType>builder()
        .put(Gf2kDokvsType.H2_BINARY_SINGLETON_GCT, Gf2eDokvsType.H2_SINGLETON_GCT)
        .put(Gf2kDokvsType.H2_BINARY_BLAZE_GCT, Gf2eDokvsType.H2_BLAZE_GCT)
        .put(Gf2kDokvsType.H3_BINARY_SINGLETON_GCT, Gf2eDokvsType.H3_SINGLETON_GCT)
        .put(Gf2kDokvsType.H3_BINARY_BLAZE_GCT, Gf2eDokvsType.H3_BLAZE_GCT)
        .put(Gf2kDokvsType.H3_CLUSTER_BINARY_BLAZE_GCT, Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT)
        .build();

    /**
     * Creates an instance.
     *
     * @param envType environment.
     * @param type    type.
     * @param n       number of key-value pairs.
     * @param keys    keys.
     * @return an instance.
     */
    public static <X> Gf2kDokvs<X> createInstance(EnvType envType, Gf2kDokvsType type, int n, byte[][] keys) {
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, getHashKeyNum(type));
        switch (type) {
            case H2_BINARY_SINGLETON_GCT:
            case H2_BINARY_BLAZE_GCT:
            case H3_BINARY_SINGLETON_GCT:
            case H3_BINARY_BLAZE_GCT:
            case H3_CLUSTER_BINARY_BLAZE_GCT:
                return new BinaryGf2kDokvs<>(envType, type, BINARY_GF2K_GF2E_TYPE_MAP.get(type), n, keys);
            case H3_FIELD_BLAZE_GCT:
                return new H3FieldBlazeGctGf2kDokvs<>(envType, n, keys);
            case H3_CLUSTER_FIELD_BLAZE_GCT:
                return new H3ClusterFieldBlazeGctGf2kDokvs<>(envType, n, keys);
            case MEGA_BIN:
                return new MegaBinGf2kDokvs<>(envType, n, keys);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets number of required hash keys.
     *
     * @param type type.
     * @return number of required hash keys.
     */
    public static int getHashKeyNum(Gf2kDokvsType type) {
        switch (type) {
            case H2_BINARY_SINGLETON_GCT:
            case H2_BINARY_BLAZE_GCT:
            case H3_BINARY_SINGLETON_GCT:
            case H3_BINARY_BLAZE_GCT:
            case H3_CLUSTER_BINARY_BLAZE_GCT:
                return Gf2eDokvsFactory.getHashKeyNum(BINARY_GF2K_GF2E_TYPE_MAP.get(type));
            case H3_FIELD_BLAZE_GCT:
                return H3FieldBlazeGctGf2kDokvs.HASH_KEY_NUM;
            case H3_CLUSTER_FIELD_BLAZE_GCT:
                return H3ClusterFieldBlazeGctGf2kDokvs.HASH_KEY_NUM;
            case MEGA_BIN:
                return Gf2eDokvsFactory.getHashKeyNum(Gf2eDokvsType.MEGA_BIN);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kDokvsType.class.getSimpleName() + ": " + type.name());
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
    public static int getM(EnvType envType, Gf2kDokvsType type, int n) {
        MathPreconditions.checkPositive("n", n);
        switch (type) {
            case H2_BINARY_SINGLETON_GCT:
            case H2_BINARY_BLAZE_GCT:
            case H3_BINARY_SINGLETON_GCT:
            case H3_BINARY_BLAZE_GCT:
            case H3_CLUSTER_BINARY_BLAZE_GCT:
                return Gf2eDokvsFactory.getM(envType, BINARY_GF2K_GF2E_TYPE_MAP.get(type), n);
            case H3_FIELD_BLAZE_GCT:
                return H3FieldBlazeGctGf2kDokvs.getLm(n) + H3FieldBlazeGctGf2kDokvs.getRm(n);
            case H3_CLUSTER_FIELD_BLAZE_GCT:
                return H3ClusterFieldBlazeGctGf2kDokvs.getM(n);
            case MEGA_BIN:
                return Gf2eDokvsFactory.getM(envType, Gf2eDokvsType.MEGA_BIN, n);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }
}
