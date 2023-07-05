package edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;

/**
 * Vectorized PIR params.
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class Mr23SingleIndexPirParams implements SingleIndexPirParams {

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
     * encryption params
     */
    private final byte[] encryptionParams;
    /**
     * first two dimension size
     */
    private final int firstTwoDimensionSize;
    /**
     * third dimension size
     */
    private final int thirdDimensionSize;

    public Mr23SingleIndexPirParams(int polyModulusDegree, int plainModulusBitLength, int firstTwoDimensionSize,
                                    int thirdDimensionSize) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = plainModulusBitLength;
        assert firstTwoDimensionSize == PirUtils.getNextPowerOfTwo(firstTwoDimensionSize);
        this.firstTwoDimensionSize = firstTwoDimensionSize;
        this.thirdDimensionSize = thirdDimensionSize;
        this.encryptionParams = Mr23SingleIndexPirNativeUtils.generateEncryptionParams(
            polyModulusDegree, plainModulusBitLength
        );
    }

    /**
     * default params
     */
    public static Mr23SingleIndexPirParams DEFAULT_PARAMS = new Mr23SingleIndexPirParams(
        8192, 20, 128, 4
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

    public int getFirstTwoDimensionSize() {
        return firstTwoDimensionSize;
    }

    public int getThirdDimensionSize() {
        return thirdDimensionSize;
    }

    @Override
    public String toString() {
        return
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
            " - size of plaintext modulus : " + plainModulusBitLength;
    }
}
