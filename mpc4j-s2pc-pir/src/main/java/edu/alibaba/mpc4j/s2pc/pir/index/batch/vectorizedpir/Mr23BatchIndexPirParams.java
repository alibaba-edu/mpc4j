package edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Vectorized Batch PIR params.
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class Mr23BatchIndexPirParams implements SingleIndexPirParams {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * plain modulus bit length
     */
    private final int plainModulusBitLength;
    /**
     * poly modulus degree
     */
    private final int polyModulusDegree;
    /**
     * encryption parameters
     */
    private final byte[] encryptionParams;
    /**
     * hash num
     */
    private final int hashNum;
    /**
     * first two dimension size
     */
    private final int firstTwoDimensionSize;
    /**
     * third dimension size
     */
    private final int thirdDimensionSize;
    /**
     * max retrieval size
     */
    private final int maxRetrievalSize;

    public Mr23BatchIndexPirParams(int polyModulusDegree, int plainModulusBitLength, int firstTwoDimensionSize,
                                   int thirdDimensionSize, int hashNum, int maxRetrievalSize) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = plainModulusBitLength;
        assert firstTwoDimensionSize == PirUtils.getNextPowerOfTwo(firstTwoDimensionSize);
        this.firstTwoDimensionSize = firstTwoDimensionSize;
        this.thirdDimensionSize = thirdDimensionSize;
        encryptionParams = Mr23BatchIndexPirNativeUtils.generateEncryptionParams(polyModulusDegree, plainModulusBitLength);
        this.hashNum = hashNum;
        this.maxRetrievalSize = maxRetrievalSize;
    }

    public static Mr23BatchIndexPirParams getParams(int elementSize, int retrievalSize) {
        MathPreconditions.checkPositive("elementSize", elementSize);
        MathPreconditions.checkPositive("retrievalSize", retrievalSize);
        int logElementSize = PirUtils.getBitLength(elementSize - 1);
        if (logElementSize <= 20) {
            logElementSize = 20;
        } else if (logElementSize <= 22) {
            logElementSize = 22;
        } else if (logElementSize <= 24) {
            logElementSize = 24;
        } else {
            return null;
        }
        int logRetrievalSize = PirUtils.getBitLength(retrievalSize - 1);
        if (logRetrievalSize < 4) {
            logRetrievalSize = 4;
        } else if (logRetrievalSize > 11) {
            return null;
        }
        Map<ByteBuffer, Mr23BatchIndexPirParams> paramsMap = new HashMap<>();
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{20, 4})), ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_16);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{20, 5})), ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_32);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{20, 6})), ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_64);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{20, 7})), ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_128);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{20, 8})), ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_256);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{20, 9})), ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_512);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{20, 10})), ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_1024);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{20, 11})), ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_2048);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{22, 4})), ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_16);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{22, 5})), ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_32);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{22, 6})), ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_64);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{22, 7})), ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_128);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{22, 8})), ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_256);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{22, 9})), ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_512);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{22, 10})), ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_1024);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{22, 11})), ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_2048);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{24, 4})), ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_16);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{24, 5})), ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_32);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{24, 6})), ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_64);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{24, 7})), ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_128);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{24, 8})), ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_256);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{24, 9})), ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_512);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{24, 10})), ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_1024);
        paramsMap.put(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{24, 11})), ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_2048);
        return paramsMap.get(ByteBuffer.wrap(IntUtils.intArrayToByteArray(new int[]{logElementSize, logRetrievalSize})));
    }

    /**
     * element size 2^20, retrieval size 16
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_16 = new Mr23BatchIndexPirParams(
        8192, 20, 128, 9, 3, 16
    );

    /**
     * element size 2^20, retrieval size 32
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_32 = new Mr23BatchIndexPirParams(
        8192, 20, 128, 5, 3, 32
    );

    /**
     * element size 2^20, retrieval size 64
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_64 = new Mr23BatchIndexPirParams(
        8192, 20, 64, 9, 3, 64
    );

    /**
     * element size 2^20, retrieval size 128
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_128 = new Mr23BatchIndexPirParams(
        8192, 20, 64, 5, 3, 128
    );

    /**
     * 2element size 2^20, retrieval size 256
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_256 = new Mr23BatchIndexPirParams(
        8192, 20, 32, 9, 3, 256
    );

    /**
     * element size 2^20, retrieval size 512
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_512 = new Mr23BatchIndexPirParams(
        8192, 20, 32, 5, 3, 512
    );

    /**
     * element size 2^20, retrieval size 1024
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_1024 = new Mr23BatchIndexPirParams(
        8192, 20, 16, 9, 3, 1024
    );

    /**
     * element size 2^20, retrieval size 2048
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_2048 = new Mr23BatchIndexPirParams(
        8192, 20, 16, 5, 3, 2048
    );

    /**
     * element size 2^22, retrieval size 16
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_16 = new Mr23BatchIndexPirParams(
        8192, 20, 256, 9, 3, 16
    );

    /**
     * 2element size 2^22, retrieval size 32
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_32 = new Mr23BatchIndexPirParams(
        8192, 20, 256, 5, 3, 32
    );

    /**
     * element size 2^22, retrieval size 64
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_64 = new Mr23BatchIndexPirParams(
        8192, 20, 128, 9, 3, 64
    );

    /**
     * element size 2^22, retrieval size 128
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_128 = new Mr23BatchIndexPirParams(
        8192, 20, 128, 5, 3, 128
    );

    /**
     * element size 2^22, retrieval size 256
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_256 = new Mr23BatchIndexPirParams(
        8192, 20, 64, 9, 3, 256
    );

    /**
     * element size 2^22, retrieval size 512
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_512 = new Mr23BatchIndexPirParams(
        8192, 20, 64, 5, 3, 512
    );

    /**
     * element size 2^22, retrieval size 1024
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_1024 = new Mr23BatchIndexPirParams(
        8192, 20, 32, 9, 3, 1024
    );

    /**
     * element size 2^22, retrieval size 2048
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_2048 = new Mr23BatchIndexPirParams(
        8192, 20, 32, 5, 3, 2048
    );

    /**
     * element size 2^24, retrieval size 16
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_16 = new Mr23BatchIndexPirParams(
        8192, 20, 512, 9, 3, 16
    );

    /**
     * element size 2^24, retrieval size 32
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_32 = new Mr23BatchIndexPirParams(
        8192, 20, 512, 5, 3, 32
    );

    /**
     * element size 2^24, retrieval size 64
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_64 = new Mr23BatchIndexPirParams(
        8192, 20, 256, 9, 3, 64
    );

    /**
     * element size 2^24, retrieval size 128
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_128 = new Mr23BatchIndexPirParams(
        8192, 20, 256, 5, 3, 128
    );

    /**
     * element size 2^24, retrieval size 256
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_256 = new Mr23BatchIndexPirParams(
        8192, 20, 128, 9, 3, 256
    );

    /**
     * element size 2^24, retrieval size 512
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_512 = new Mr23BatchIndexPirParams(
        8192, 20, 128, 5, 3, 512
    );

    /**
     * element size 2^24, retrieval size 1024
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_1024 = new Mr23BatchIndexPirParams(
        8192, 20, 64, 9, 3, 1024
    );

    /**
     * element size 2^24, retrieval size 2048
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_2048 = new Mr23BatchIndexPirParams(
        8192, 20, 64, 5, 3, 2048
    );

    @Override
    public int getPlainModulusBitLength() {
        return plainModulusBitLength;
    }

    @Override
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    @Override
    public int getDimension() {
        return 3;
    }

    @Override
    public byte[] getEncryptionParams() {
        return encryptionParams;
    }

    @Override
    public String toString() {
        return
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
            " - size of plaintext modulus : " + plainModulusBitLength;
    }

    public int getFirstTwoDimensionSize() {
        return firstTwoDimensionSize;
    }

    public int getThirdDimensionSize() {
        return thirdDimensionSize;
    }

    /**
     * return hash num.
     *
     * @return hash num.
     */
    public int getHashNum() {
        return hashNum;
    }

    /**
     * return max retrieval size.
     *
     * @return max retrieval size.
     */
    public int getMaxRetrievalSize() {
        return maxRetrievalSize;
    }
}
