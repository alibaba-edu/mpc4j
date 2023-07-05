package edu.alibaba.mpc4j.s2pc.pir.index.single.xpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;

/**
 * XPIR params.
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class Mbfk16SingleIndexPirParams implements SingleIndexPirParams {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
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

    public Mbfk16SingleIndexPirParams(int polyModulusDegree, int plainModulusBitLength, int dimension) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = plainModulusBitLength;
        this.dimension = dimension;
        this.encryptionParams = Mbfk16SingleIndexPirNativeUtils.generateEncryptionParams(
            polyModulusDegree, (1L << plainModulusBitLength) + 1
        );
        this.expansionRatio = Mbfk16SingleIndexPirNativeUtils.expansionRatio(this.encryptionParams);
    }

    /**
     * default params
     */
    public static Mbfk16SingleIndexPirParams DEFAULT_PARAMS = new Mbfk16SingleIndexPirParams(4096, 20, 2);

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
        return dimension;
    }

    @Override
    public byte[] getEncryptionParams() {
        return encryptionParams;
    }

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
