package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * private equality test utilities.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
class PeqtTestUtils {
    /**
     * private constructor.
     */
    private PeqtTestUtils() {
        // empty
    }

    static byte[][] genSenderInputArray(int l, int num, SecureRandom secureRandom) {
        int byteL = CommonUtils.getByteLength(l);
        return IntStream.range(0, num)
            .parallel()
            .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, secureRandom))
            .toArray(byte[][]::new);
    }

    static byte[][] genReceiverInputArray(int l, byte[][] inputArray, SecureRandom secureRandom) {
        int byteL = CommonUtils.getByteLength(l);
        int num = inputArray.length;
        return IntStream.range(0, num)
            .parallel()
            .mapToObj(index -> {
                boolean equal = (index % 2 == 0);
                if (equal) {
                    return BytesUtils.clone(inputArray[index]);
                } else {
                    return BytesUtils.randomByteArray(byteL, l, secureRandom);
                }
            })
            .toArray(byte[][]::new);
    }
}
