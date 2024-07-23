package edu.alibaba.mpc4j.s2pc.pir.stdpir.index;

/**
 * standard index PIR parameters.
 *
 * @author Weiran Liu
 * @date 2024/7/9
 */
public interface StdIdxPirParams {
    /**
     * Gets plaintext modulus bit length.
     *
     * @return plaintext modulus bit length.
     */
    int getPlainModulusBitLength();

    /**
     * Gets degree of polynomial modulus.
     *
     * @return degree of polynomial modulus.
     */
    int getPolyModulusDegree();

    /**
     * Gets dimension used to encode the database.
     *
     * @return dimension.
     */
    int getDimension();

    /**
     * Gets encryption params.
     *
     * @return encryption params.
     */
    byte[] getEncryptionParams();
}
