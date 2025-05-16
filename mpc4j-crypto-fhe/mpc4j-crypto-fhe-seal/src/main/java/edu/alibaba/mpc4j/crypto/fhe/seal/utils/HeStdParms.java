package edu.alibaba.mpc4j.crypto.fhe.seal.utils;

/**
 * Largest allowed bit counts for coeff_modulus (modulus in the ciphertext space) based on the security estimates from
 * homomorphicencryption.org security standard. Microsoft SEAL samples the secret key from a ternary {-1, 0, 1}
 * distribution.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/hestdparms.h">hestdparms.h</a>.
 * <p>
 * The standard can be found at:
 * <p>
 * <a href="https://homomorphicencryption.org/wp-content/uploads/2018/11/HomomorphicEncryptionStandardv1.1.pdf">
 * HomomorphicEncryptionStandardv1.1.pdf
 * </a>
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
     * Standard deviation for error distribution, 𝜎 = 8 / √(2π) ≈ 3.2. See Section 8.3 of the paper
     * <a href="https://www.microsoft.com/en-us/research/uploads/prod/2017/11/sealmanual-2-3-1.pdf">sealmanual-2-3-1.pdf</a>
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
        return switch (polyModulusDegree) {
            case 1024 -> 27;
            case 2048 -> 54;
            case 4096 -> 109;
            case 8192 -> 218;
            case 16384 -> 438;
            case 32768 -> 881;
            // add for CKKS bootstrapping, see
            // https://github.com/zju-abclab/NEXUS/blob/main/thirdparty/SEAL-4.1-bs/native/src/seal/util/hestdparms.h#L35
            case 65536 -> 1792;
            default -> 0;
        };
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
        return switch (polyModulusDegree) {
            case 1024 -> 19;
            case 2048 -> 37;
            case 4096 -> 75;
            case 8192 -> 152;
            case 16384 -> 305;
            case 32768 -> 611;
            default -> 0;
        };
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
        return switch (polyModulusDegree) {
            case 1024 -> 14;
            case 2048 -> 29;
            case 4096 -> 58;
            case 8192 -> 118;
            case 16384 -> 237;
            case 32768 -> 476;
            default -> 0;
        };
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
        return switch (polyModulusDegree) {
            case 1024 -> 25;
            case 2048 -> 51;
            case 4096 -> 101;
            case 8192 -> 202;
            case 16384 -> 411;
            case 32768 -> 827;
            default -> 0;
        };
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
        return switch (polyModulusDegree) {
            case 1024 -> 17;
            case 2048 -> 35;
            case 4096 -> 70;
            case 8192 -> 141;
            case 16384 -> 284;
            case 32768 -> 571;
            default -> 0;
        };
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
        return switch (polyModulusDegree) {
            case 1024 -> 13;
            case 2048 -> 27;
            case 4096 -> 54;
            case 8192 -> 109;
            case 16384 -> 220;
            case 32768 -> 443;
            default -> 0;
        };
    }
}
