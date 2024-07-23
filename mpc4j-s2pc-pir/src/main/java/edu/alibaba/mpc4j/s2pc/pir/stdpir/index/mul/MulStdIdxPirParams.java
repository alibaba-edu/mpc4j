package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.mul;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirParams;

/**
 * MulPIR params.
 *
 * @author Qixian Zhou
 * @date 2023/5/29
 */
public class MulStdIdxPirParams implements StdIdxPirParams {

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

    public MulStdIdxPirParams(int polyModulusDegree, int plainModulusBitLength, int dimension) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = plainModulusBitLength;
        this.dimension = dimension;
        this.encryptionParams = MulStdIdxPirNativeUtils.generateEncryptionParams(
            polyModulusDegree, (1L << plainModulusBitLength) + 1
        );
    }

    /**
     * default params
     */
    public static MulStdIdxPirParams DEFAULT_PARAMS = new MulStdIdxPirParams(8192, 24, 3);

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

    @Override
    public String toString() {
        return
            "SEAL encryption parameters : " + "\n" +
                " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
                " - size of plaintext modulus : " + plainModulusBitLength + "\n" +
                " - dimension : " + dimension;
    }
}