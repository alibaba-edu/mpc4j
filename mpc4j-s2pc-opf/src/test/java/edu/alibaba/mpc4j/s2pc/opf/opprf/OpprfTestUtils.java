package edu.alibaba.mpc4j.s2pc.opf.opprf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.SimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Batch OPPRF test utilities.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public class OpprfTestUtils {
    /**
     * input byte length
     */
    private static final int INPUT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * private constructor.
     */
    private OpprfTestUtils() {
        // empty
    }

    public static byte[][][] generateSenderInputArrays(int batchNum, int pointNum, SecureRandom secureRandom) {
        byte[][] keys = CommonUtils.generateRandomKeys(1, secureRandom);
        // use simple hash to place int into batched queries.
        SimpleIntHashBin simpleIntHashBin = new SimpleIntHashBin(EnvType.STANDARD, batchNum, pointNum, keys);
        simpleIntHashBin.insertItems(IntStream.range(0, pointNum).toArray());
        byte[][][] inputArrays = new byte[batchNum][][];
        for (int batchIndex = 0; batchIndex < batchNum; batchIndex++) {
            int batchPointNum = simpleIntHashBin.binSize(batchIndex);
            inputArrays[batchIndex] = new byte[batchPointNum][];
            for (int pointIndex = 0; pointIndex < batchPointNum; pointIndex++) {
                inputArrays[batchIndex][pointIndex] = new byte[INPUT_BYTE_LENGTH];
                secureRandom.nextBytes(inputArrays[batchIndex][pointIndex]);
            }
        }
        return inputArrays;
    }

    public static byte[][][] generateDistinctSenderTargetArrays(int l, byte[][][] inputArrays, SecureRandom secureRandom) {
        int byteL = CommonUtils.getByteLength(l);
        int batchNum = inputArrays.length;
        byte[][][] targetArrays = new byte[batchNum][][];
        for (int batchIndex = 0; batchIndex < batchNum; batchIndex++) {
            int batchPointNum = inputArrays[batchIndex].length;
            targetArrays[batchIndex] = new byte[batchPointNum][];
            for (int pointIndex = 0; pointIndex < batchPointNum; pointIndex++) {
                targetArrays[batchIndex][pointIndex] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            }
        }
        return targetArrays;
    }

    public static byte[][][] generateEqualSenderTargetArrays(int l, byte[][][] inputArrays, SecureRandom secureRandom) {
        int byteL = CommonUtils.getByteLength(l);
        int batchNum = inputArrays.length;
        byte[][][] targetArrays = new byte[batchNum][][];
        for (int batchIndex = 0; batchIndex < batchNum; batchIndex++) {
            int batchPointNum = inputArrays[batchIndex].length;
            targetArrays[batchIndex] = new byte[batchPointNum][];
            byte[] target = BytesUtils.randomByteArray(byteL, l, secureRandom);
            for (int pointIndex = 0; pointIndex < batchPointNum; pointIndex++) {
                targetArrays[batchIndex][pointIndex] = BytesUtils.clone(target);
            }
        }
        return targetArrays;
    }

    public static byte[][] generateReceiverInputArray(int l, byte[][][] inputArrays, SecureRandom secureRandom) {
        int byteL = CommonUtils.getByteLength(l);
        int batchNum = inputArrays.length;
        byte[][] inputArray = new byte[batchNum][];
        for (int batchIndex = 0; batchIndex < batchNum; batchIndex++) {
            int batchPointNum = inputArrays[batchIndex].length;
            if (batchPointNum > 0) {
                // batch point num is not zero
                if (batchIndex % 2 == 0) {
                    // randomly select a point to be the input
                    int pointIndex = secureRandom.nextInt(batchPointNum);
                    inputArray[batchIndex] = BytesUtils.clone(inputArrays[batchIndex][pointIndex]);
                } else {
                    // randomly generate a input
                    inputArray[batchIndex] = BytesUtils.randomByteArray(byteL, l, secureRandom);
                }
            } else {
                // batch point num is zero, create a random input
                inputArray[batchIndex] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            }
        }
        return inputArray;
    }
}
