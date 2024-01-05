package edu.alibaba.mpc4j.crypto.fhe.utils;

/**
 * Largest allowed bit counts for coeff_modulus (modulus in the ciphertext space) based on the security estimates from
 * homomorphicencryption.org security standard. Microsoft SEAL samples the secret key from a ternary {-1, 0, 1}
 * distribution.
 * <p></p>
 * The implementation is from: https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/hestdparms.h
 * <p></p>
 * The standard can be found at:
 * <p>
 * https://homomorphicencryption.org/wp-content/uploads/2018/11/HomomorphicEncryptionStandardv1.1.pdf
 * </p>
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/29
 */
public class HeStdParms {
    /**
     * private constructor.
     */
    private HeStdParms() {
        // empty
    }
    /**
     * Standard deviation for error distribution, 𝜎 = 8 / √(2π) ≈ 3.2. See Section 8.3 of the following paper:
     * <p>
     * https://www.microsoft.com/en-us/research/uploads/prod/2017/11/sealmanual-2-3-1.pdf
     * </p>
     */
    public final static double HE_STD_PARMS_ERROR_STD_DEV = 3.2;

    /**
     * Gets the largest allowed bit counts for coeff_modulus (modulus in the ciphertext space) based on the 128-bit
     * security estimates from homomorphicencryption.org security standard, where the secret key is from a ternary
     * {-1, 0, 1} distribution. See P27 of the standard. Returns 0 for invalid polynomial modulus degree.
     *
     * @param polyModulusDegree N.
     * @return the largest allowed bit counts for coeff_modulus.
     */
    public static int heStdParms128Tc(int polyModulusDegree) {
        switch (polyModulusDegree) {
            case 1024:
                return 27;
            case 2048:
                return 54;
            case 4096:
                return 109;
            case 8192:
                return 218;
            case 16384:
                return 438;
            case 32768:
                return 881;
            default:
                return 0;
        }
    }

    /**
     * Gets the largest allowed bit counts for coeff_modulus (modulus in the ciphertext space) based on the 192-bit
     * security estimates from homomorphicencryption.org security standard, where the secret key is from a ternary
     * {-1, 0, 1} distribution. See P27 of the standard. Returns 0 for invalid polynomial modulus degree.
     *
     * @param polyModulusDegree N.
     * @return the largest allowed bit counts for coeff_modulus.
     */
    public static int heStdParms192Tc(int polyModulusDegree) {
        switch (polyModulusDegree) {
            case 1024:
                return 19;
            case 2048:
                return 37;
            case 4096:
                return 75;
            case 8192:
                return 152;
            case 16384:
                return 305;
            case 32768:
                return 611;
            default:
                return 0;
        }
    }

    /**
     * Gets the largest allowed bit counts for coeff_modulus (modulus in the ciphertext space) based on the 256-bit
     * security estimates from homomorphicencryption.org security standard, where the secret key is from a ternary
     * {-1, 0, 1} distribution. See P27 of the standard. Returns 0 for invalid polynomial modulus degree.
     *
     * @param polyModulusDegree N.
     * @return the largest allowed bit counts for coeff_modulus.
     */
    public static int heStdParms256Tc(int polyModulusDegree) {
        switch (polyModulusDegree) {
            case 1024:
                return 14;
            case 2048:
                return 29;
            case 4096:
                return 58;
            case 8192:
                return 118;
            case 16384:
                return 237;
            case 32768:
                return 476;
            default:
                return 0;
        }
    }

    /**
     * Gets the largest allowed bit counts for coeff_modulus (modulus in the ciphertext space) based on the 128-bit
     * quantum security estimates from homomorphicencryption.org security standard, where the secret key is from a
     * ternary {-1, 0, 1} distribution. See P28 of the standard. Returns 0 for invalid polynomial modulus degree.
     *
     * @param polyModulusDegree N.
     * @return the largest allowed bit counts for coeff_modulus.
     */
    public static int heStdParms128Tq(int polyModulusDegree) {
        switch (polyModulusDegree) {
            case 1024:
                return 25;
            case 2048:
                return 51;
            case 4096:
                return 101;
            case 8192:
                return 202;
            case 16384:
                return 411;
            case 32768:
                return 827;
            default:
                return 0;
        }
    }

    /**
     * Gets the largest allowed bit counts for coeff_modulus (modulus in the ciphertext space) based on the 192-bit
     * quantum security estimates from homomorphicencryption.org security standard, where the secret key is from a
     * ternary {-1, 0, 1} distribution. See P28-P29 of the standard. Returns 0 for invalid polynomial modulus degree.
     *
     * @param polyModulusDegree N.
     * @return the largest allowed bit counts for coeff_modulus.
     */
    public static int heStdParms192Tq(int polyModulusDegree) {
        switch (polyModulusDegree) {
            case 1024:
                return 17;
            case 2048:
                return 35;
            case 4096:
                return 70;
            case 8192:
                return 141;
            case 16384:
                return 284;
            case 32768:
                return 571;
            default:
                return 0;
        }
    }

    /**
     * Gets the largest allowed bit counts for coeff_modulus (modulus in the ciphertext space) based on the 256-bit
     * quantum security estimates from homomorphicencryption.org security standard, where the secret key is from a
     * ternary {-1, 0, 1} distribution. See P28-P29 of the standard. Returns 0 for invalid polynomial modulus degree.
     *
     * @param polyModulusDegree N.
     * @return the largest allowed bit counts for coeff_modulus.
     */
    public static int heStdParms256Tq(int polyModulusDegree) {
        switch (polyModulusDegree) {
            case 1024:
                return 13;
            case 2048:
                return 27;
            case 4096:
                return 54;
            case 8192:
                return 109;
            case 16384:
                return 220;
            case 32768:
                return 443;
            default:
                return 0;
        }
    }
}
