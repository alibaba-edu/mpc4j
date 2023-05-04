package edu.alibaba.mpc4j.s2pc.pir.index;

/**
 * Index PIR params interface.
 *
 * @author Liqiang Peng
 * @date 2023/3/1
 */
public interface IndexPirParams {

    /**
     * plain modulus size.
     *
     * @return plain modulus size.
     */
    int getPlainModulusBitLength();

    /**
     * poly modulus degree.
     *
     * @return poly modulus degree.
     */
    int getPolyModulusDegree();

    /**
     * dimension.
     *
     * @return dimension.
     */
    int getDimension();

    /**
     * encryption params.
     *
     * @return encryption params.
     */
    byte[] getEncryptionParams();
}
