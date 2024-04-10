package edu.alibaba.mpc4j.common.structure.okve.dokvs;

import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

/**
 * DOKVS using garbled cuckoo table with 3 hash functions utils.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
public class H3GctDokvsUtils {
    /**
     * private constructor.
     */
    private H3GctDokvsUtils() {
        // empty
    }

    /**
     * number of sparse hashes
     */
    public static final int SPARSE_HASH_NUM = 3;
    /**
     * number of hash keys
     */
    public static final int HASH_KEY_NUM = 2;

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
        // check the output byte length of the hash function
        assert hash.getOutputByteLength() == SPARSE_HASH_NUM * Integer.BYTES;
        assert SPARSE_HASH_NUM <= m;
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int[] sparsePositions = IntUtils.byteArrayToIntArray(hash.getBytes(keyBytes));
        // we now use the method provided in VOLE-PSI to get distinct hash indexes
        sparsePositions[0] = Math.abs(sparsePositions[0] % m);
        sparsePositions[1] = Math.abs(sparsePositions[1] % (m - 1));
        sparsePositions[2] = Math.abs(sparsePositions[2] % (m - 2));

        int min = Math.min(sparsePositions[0], sparsePositions[1]);
        int max = sparsePositions[0] + sparsePositions[1] - min;
        if (max == sparsePositions[1]) {
            sparsePositions[1]++;
            max++;
        }
        if (sparsePositions[2] >= min) {
            sparsePositions[2]++;
        }
        if (sparsePositions[2] >= max) {
            sparsePositions[2]++;
        }
        return sparsePositions;
    }
}
