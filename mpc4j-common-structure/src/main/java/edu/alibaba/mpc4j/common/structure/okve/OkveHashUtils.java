package edu.alibaba.mpc4j.common.structure.okve;

import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

/**
 * OKVE hash utilities.
 *
 * @author Weiran Liu
 * @date 2024/7/25
 */
public class OkveHashUtils {
    /**
     * private constructor.
     */
    private OkveHashUtils() {
        // empty
    }

    /**
     * Computes distinct positions. The implementation is inspired by libOTe. For details, see buildRow of
     * cryptoTools/Common/CuckooIndex.cpp.
     *
     * @param hash   hash.
     * @param key    key.
     * @param weight number of outputs.
     * @param bound  upper bound for the output range.
     * @param <X>    key type.
     * @return distinct positions.
     */
    public static <X> int[] distinctPositions(Prf hash, X key, int weight, int bound) {
        assert hash.getOutputByteLength() == weight * Integer.BYTES;
        assert weight > 0 && weight <= bound;
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int[] sparsePositions = IntUtils.byteArrayToIntArray(hash.getBytes(keyBytes));
        if (weight == 2) {
            sparsePositions[0] = Math.abs(sparsePositions[0] % bound);
            sparsePositions[1] = Math.abs(sparsePositions[1] % (bound - 1));
            if (sparsePositions[1] >= sparsePositions[0]) {
                sparsePositions[1]++;
            }
        } else if (weight == 3) {
            sparsePositions[0] = Math.abs(sparsePositions[0] % bound);
            sparsePositions[1] = Math.abs(sparsePositions[1] % (bound - 1));
            sparsePositions[2] = Math.abs(sparsePositions[2] % (bound - 2));
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
        } else {
            for (int j = 0; j < weight; j++) {
                // hj = r % (m - j)
                int colIdx = Math.abs(sparsePositions[j] % (bound - j));
                int iterIndex = 0;
                int endIndex = j;
                // for each previous hi <= hj, we set hj = hj + 1.
                while (iterIndex != endIndex) {
                    if (sparsePositions[iterIndex] <= colIdx) {
                        colIdx++;
                    } else {
                        break;
                    }
                    iterIndex++;
                }
                // now we now that all hi > hj, we place the value
                while (iterIndex < endIndex) {
                    sparsePositions[endIndex] = sparsePositions[endIndex - 1];
                    endIndex--;
                }
                sparsePositions[iterIndex] = colIdx;
            }
        }
        return sparsePositions;
    }
}
