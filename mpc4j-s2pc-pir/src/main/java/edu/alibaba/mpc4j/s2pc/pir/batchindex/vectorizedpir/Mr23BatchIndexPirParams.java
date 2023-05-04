package edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Vectorized Batch PIR协议参数。
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class Mr23BatchIndexPirParams implements IndexPirParams {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * 明文模数比特长度
     */
    private final int plainModulusBitLength;
    /**
     * 多项式阶
     */
    private final int polyModulusDegree;
    /**
     * SEAL上下文参数
     */
    private final byte[] encryptionParams;
    /**
     * 哈希数目
     */
    private final int hashNum;
    /**
     * 前两维长度
     */
    private final int firstTwoDimensionSize;
    /**
     * 第三维长度
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
        // 生成加密方案参数
        encryptionParams = Mr23BatchIndexPirNativeUtils.generateSealContext(polyModulusDegree, plainModulusBitLength);
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
     * 2^20，适合分桶数目16
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_16 = new Mr23BatchIndexPirParams(
        8192, 20, 128, 9, 3, 16
    );

    /**
     * 2^20，适合分桶数目32
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_32 = new Mr23BatchIndexPirParams(
        8192, 20, 128, 5, 3, 32
    );

    /**
     * 2^20，适合分桶数目64
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_64 = new Mr23BatchIndexPirParams(
        8192, 20, 64, 9, 3, 64
    );

    /**
     * 2^20，适合分桶数目128
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_128 = new Mr23BatchIndexPirParams(
        8192, 20, 64, 5, 3, 128
    );

    /**
     * 2^20，适合分桶数目256
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_256 = new Mr23BatchIndexPirParams(
        8192, 20, 32, 9, 3, 256
    );

    /**
     * 2^20，适合分桶数目512
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_512 = new Mr23BatchIndexPirParams(
        8192, 20, 32, 5, 3, 512
    );

    /**
     * 2^20，适合分桶数目1024
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_1024 = new Mr23BatchIndexPirParams(
        8192, 20, 16, 9, 3, 1024
    );

    /**
     * 2^20，适合分桶数目2048
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_2048 = new Mr23BatchIndexPirParams(
        8192, 20, 16, 5, 3, 2048
    );

    /**
     * 2^22，适合分桶数目16
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_16 = new Mr23BatchIndexPirParams(
        8192, 20, 256, 9, 3, 16
    );

    /**
     * 2^22，适合分桶数目32
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_32 = new Mr23BatchIndexPirParams(
        8192, 20, 256, 5, 3, 32
    );

    /**
     * 2^22，适合分桶数目64
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_64 = new Mr23BatchIndexPirParams(
        8192, 20, 128, 9, 3, 64
    );

    /**
     * 2^22，适合分桶数目128
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_128 = new Mr23BatchIndexPirParams(
        8192, 20, 128, 5, 3, 128
    );

    /**
     * 2^22，适合分桶数目256
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_256 = new Mr23BatchIndexPirParams(
        8192, 20, 64, 9, 3, 256
    );

    /**
     * 2^22，适合分桶数目512
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_512 = new Mr23BatchIndexPirParams(
        8192, 20, 64, 5, 3, 512
    );

    /**
     * 2^22，适合分桶数目1024
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_1024 = new Mr23BatchIndexPirParams(
        8192, 20, 32, 9, 3, 1024
    );

    /**
     * 2^22，适合分桶数目2048
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_2048 = new Mr23BatchIndexPirParams(
        8192, 20, 32, 5, 3, 2048
    );

    /**
     * 2^24，适合分桶数目16
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_16 = new Mr23BatchIndexPirParams(
        8192, 20, 512, 9, 3, 16
    );

    /**
     * 2^24，适合分桶数目32
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_32 = new Mr23BatchIndexPirParams(
        8192, 20, 512, 5, 3, 32
    );

    /**
     * 2^24，适合分桶数目64
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_64 = new Mr23BatchIndexPirParams(
        8192, 20, 256, 9, 3, 64
    );

    /**
     * 2^24，适合分桶数目128
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_128 = new Mr23BatchIndexPirParams(
        8192, 20, 256, 5, 3, 128
    );

    /**
     * 2^24，适合分桶数目256
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_256 = new Mr23BatchIndexPirParams(
        8192, 20, 128, 9, 3, 256
    );

    /**
     * 2^24，适合分桶数目512
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_512 = new Mr23BatchIndexPirParams(
        8192, 20, 128, 5, 3, 512
    );

    /**
     * 2^24，适合分桶数目1024
     */
    public static Mr23BatchIndexPirParams ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_1024 = new Mr23BatchIndexPirParams(
        8192, 20, 64, 9, 3, 1024
    );

    /**
     * 2^24，适合分桶数目2048
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
     * 返回哈希数目。
     *
     * @return 哈希数目。
     */
    public int getHashNum() {
        return hashNum;
    }

    public int getMaxRetrievalSize() {
        return maxRetrievalSize;
    }
}
