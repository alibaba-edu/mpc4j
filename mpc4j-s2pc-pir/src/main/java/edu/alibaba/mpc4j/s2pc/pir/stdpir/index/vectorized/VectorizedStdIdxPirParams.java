package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirParams;

/**
 * Vectorized Batch PIR params.
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class VectorizedStdIdxPirParams implements StdIdxPirParams {

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
    public int firstTwoDimensionSize;
    /**
     * third dimension size
     */
    public int thirdDimensionSize;
    /**
     * gap
     */
    public int gap;
    /**
     * row size
     */
    public int rowSize;

    public VectorizedStdIdxPirParams(int polyModulusDegree, int plainModulusBitLength) {
        assert polyModulusDegree >= 1 << 13;
        this.polyModulusDegree = polyModulusDegree;
        assert plainModulusBitLength < Integer.SIZE;
        this.plainModulusBitLength = plainModulusBitLength;
        this.rowSize = polyModulusDegree / 2;
        this.encryptionParams = VectorizedStdIdxPirNativeUtils.generateEncryptionParams(
            polyModulusDegree, plainModulusBitLength
        );
    }

    /**
     * default params
     */
    public static VectorizedStdIdxPirParams DEFAULT_PARAMS = new VectorizedStdIdxPirParams(
        8192, 28
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

    public int[] getDimensionSize() {
        return new int[] {firstTwoDimensionSize, firstTwoDimensionSize, thirdDimensionSize};
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
            " - size of plaintext modulus : " + plainModulusBitLength + "\n" +
            " - first two dimension size : " + firstTwoDimensionSize + "\n" +
            " - third dimension size :" + thirdDimensionSize;
    }

    public void calculateDimensions(int num) {
        int cubeRoot = (int) Math.ceil(Math.cbrt(num));
        firstTwoDimensionSize = PirUtils.getNextPowerOfTwo(cubeRoot);
        assert firstTwoDimensionSize <= polyModulusDegree / 2 : "first two dimensions exceed polynomial degree";
        thirdDimensionSize = (int) Math.ceil(num / Math.pow(firstTwoDimensionSize, 2));
        this.gap = rowSize / firstTwoDimensionSize;
    }
}
