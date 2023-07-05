package edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Millionaire protocol test utilities.
 *
 * @author Li Peng
 * @date 2023/5/11
 */
public class MillionaireTestUtils {
    /**
     * private constructor.
     */
    private MillionaireTestUtils() {
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
                .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, secureRandom))
                .toArray(byte[][]::new);
    }
}
