package edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;

/**
 * OnionPIR params.
 *
 * @author Liqiang Peng
 * @date 2022/11/11
 */
public class Mcr21SingleIndexPirParams implements SingleIndexPirParams {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * first dimension size
     */
    private final int firstDimensionSize;
    /**
     * subsequent dimension size
     */
    public final int SUBSEQUENT_DIMENSION_SIZE = 4;
    /**
     * plain modulus size
     */
    private static final int PLAIN_MODULUS_BIT_LENGTH = 54;
    /**
     * poly modulus degree
     */
    private static final int POLY_MODULUS_DEGREE = 4096;
    /**
     * SEAL encryption params
     */
    private final byte[] encryptionParams;

    public Mcr21SingleIndexPirParams(int firstDimensionSize) {
        assert (firstDimensionSize <= 512) : "first dimension is too large";
        this.firstDimensionSize = firstDimensionSize;
        this.encryptionParams = Mcr21SingleIndexPirNativeUtils.generateEncryptionParams(
            POLY_MODULUS_DEGREE, PLAIN_MODULUS_BIT_LENGTH
        );
    }

    /**
     * default params
     */
    public static Mcr21SingleIndexPirParams DEFAULT_PARAMS = new Mcr21SingleIndexPirParams(32);

    @Override
    public int getPlainModulusBitLength() {
        return PLAIN_MODULUS_BIT_LENGTH;
    }

    @Override
    public int getPolyModulusDegree() {
        return POLY_MODULUS_DEGREE;
    }

    @Override
    public int getDimension() {
        return firstDimensionSize;
    }

    public int getFirstDimensionSize() {
        return firstDimensionSize;
    }

    public int getSubsequentDimensionSize() {
        return SUBSEQUENT_DIMENSION_SIZE;
    }

    @Override
    public byte[] getEncryptionParams() {
        return encryptionParams;
    }

    /**
     * GSW scheme decomposition size.
     *
     * @return decomposition size.
     */
    public int getGswDecompSize() {
        return 7;
    }

    @Override
    public String toString() {
        return
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + POLY_MODULUS_DEGREE + "\n" +
            " - size of plaintext modulus : " + PLAIN_MODULUS_BIT_LENGTH  + "\n" +
                " - first dimension size : " + firstDimensionSize;
    }
}