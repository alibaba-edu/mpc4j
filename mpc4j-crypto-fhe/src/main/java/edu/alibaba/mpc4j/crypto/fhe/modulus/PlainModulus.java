package edu.alibaba.mpc4j.crypto.fhe.modulus;

/**
 * This class contains static methods for creating a plaintext modulus easily.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/src/seal/modulus.h#L523
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/29
 */
public class PlainModulus {
    /**
     * Creates a prime number Modulus for use as plain_modulus encryption parameter that supports batching with a given
     * poly_modulus_degree.
     *
     * @param polyModulusDegree the value of the poly_modulus_degree encryption parameter (N).
     * @param bitSize           the bit-length of the prime to be generated.
     * @return a prime number modulus for use as plain_modulus encryption parameter.
     */
    public static Modulus batching(int polyModulusDegree, int bitSize) {
        return CoeffModulus.create(polyModulusDegree, bitSize);
    }

    /**
     * Creates several prime number Modulus elements that can be used as plain_modulus encryption parameters, each
     * supporting batching with a given poly_modulus_degree.
     *
     * @param polyModulusDegree the value of the poly_modulus_degree encryption parameter (N).
     * @param bitSizes          the bit-lengths of the primes to be generated.
     * @return a prime number modulus array for use as plain_modulus encryption parameter.
     */
    public static Modulus[] batching(int polyModulusDegree, int[] bitSizes) {
        return CoeffModulus.create(polyModulusDegree, bitSizes);
    }
}
