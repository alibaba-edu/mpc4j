package edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;

import java.math.BigInteger;

/**
 * FastPIR params.
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class Ayaa21SingleIndexPirParams implements SingleIndexPirParams {

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

    public Ayaa21SingleIndexPirParams(int polyModulusDegree, long plainModulus, long[] coeffModulus) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = BigInteger.valueOf(plainModulus).bitLength() - 1;
        this.encryptionParams = Ayaa21SingleIndexPirNativeUtils.generateEncryptionParams(
            polyModulusDegree, plainModulus, coeffModulus
        );
    }

    /**
     * default params
     */
    public static Ayaa21SingleIndexPirParams DEFAULT_PARAMS = new Ayaa21SingleIndexPirParams(
        4096,
        1073153L,
        new long[]{1152921504606830593L, 562949953216513L}
    );

    @Override
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    @Override
    public int getDimension() {
        return 2;
    }

    @Override
    public int getPlainModulusBitLength() {
        return plainModulusBitLength;
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
}
