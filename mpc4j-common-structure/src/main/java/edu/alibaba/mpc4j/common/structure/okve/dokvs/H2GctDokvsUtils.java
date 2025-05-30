package edu.alibaba.mpc4j.common.structure.okve.dokvs;

import edu.alibaba.mpc4j.common.structure.okve.OkveHashUtils;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;

/**
 * DOKVS using garbled cuckoo table with 2 hash functions utils.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
public class H2GctDokvsUtils {
    /**
     * private constructor
     */
    private H2GctDokvsUtils() {
        // empty
    }

    /**
     * number of sparse hashes
     */
    public static final int SPARSE_HASH_NUM = 2;
    /**
     * number of hash keys
     */
    public static int HASH_KEY_NUM = 2;

    /**
     * Computes sparse positions for the given key.
     *
     * @param hash hash function.
     * @param key  key.
     * @param m    sparse position bound.
     * @param <X>  key type.
     * @return sparse positions for the given key.
     */
    public static <X> int[] sparsePositions(Prf hash, X key, int m) {
        return OkveHashUtils.distinctPositions(hash, key, SPARSE_HASH_NUM, m);
    }
}
