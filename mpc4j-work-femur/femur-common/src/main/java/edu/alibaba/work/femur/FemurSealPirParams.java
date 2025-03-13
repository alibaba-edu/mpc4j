package edu.alibaba.work.femur;

/**
 * SEAL Femur demo PIR params.
 *
 * @author Liqiang Peng
 * @date 2024/9/19
 */
public class FemurSealPirParams {

    static {
        System.loadLibrary("femur-native-fhe");
    }

    /**
     * plain modulus size
     */
    private final int plainModulusBitLength;
    /**
     * poly modulus degree
     */
    private final int polyModulusDegree;
    /**
     * dimension
     */
    private final int dimension;
    /**
     * SEAL encryption params
     */
    private final byte[] encryptionParams;
    /**
     * expansion ratio
     */
    private final int expansionRatio;

    public FemurSealPirParams(int polyModulusDegree, int plainModulusBitLength, int dimension) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = plainModulusBitLength;
        this.dimension = dimension;
        this.encryptionParams = FemurSealPirNativeUtils.generateEncryptionParams(
            polyModulusDegree, (1L << plainModulusBitLength) + 1
        );
        this.expansionRatio = FemurSealPirNativeUtils.expansionRatio(this.encryptionParams);
    }

    /**
     * default params
     */
    public static FemurSealPirParams DEFAULT_PARAMS = new FemurSealPirParams(4096, 20, 2);

    /**
     * Get plain modulus bit length.
     *
     * @return plain modulus bit length.
     */
    public int getPlainModulusBitLength() {
        return plainModulusBitLength;
    }

    /**
     * Get poly modulus degree.
     *
     * @return poly modulus degree.
     */
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    /**
     * Get dimension.
     *
     * @return dimension.
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * Get encryption params.
     *
     * @return encryption params.
     */
    public byte[] getEncryptionParams() {
        return encryptionParams;
    }

    /**
     * Get expansion ratio.
     *
     * @return expansion ratio.
     */
    public int getExpansionRatio() {
        return expansionRatio;
    }

    @Override
    public String toString() {
        return
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
            " - size of plaintext modulus : " + plainModulusBitLength + "\n" +
            " - dimension : " + dimension;
    }
}
