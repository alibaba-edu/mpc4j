package edu.alibaba.mpc4j.work.psipir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.util.List;

/**
 * PSI-PIR native utils.
 *
 * @author Liqiang Peng
 * @date 2023/3/9
 */
public class Lpzl24BatchPirNativeUtils {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    private Lpzl24BatchPirNativeUtils() {
        // empty
    }

    /**
     * generate encryption parameters.
     *
     * @param polyModulusDegree poly modulus degree.
     * @param plainModulus      plain modulus.
     * @param coeffModulusBits  coeff modulus bits.
     * @return encryption parameters.
     */
    static native List<byte[]> genEncryptionParameters(int polyModulusDegree, long plainModulus, int[] coeffModulusBits);

    /**
     * process database.
     *
     * @param encryptionParameters encryption parameters.
     * @param coeffs               coefficients.
     * @param psLowDegree          Paterson-Stockmeyer low degree.
     * @return plaintexts in NTT form.
     */
    static native List<byte[]> processDatabase(byte[] encryptionParameters, long[][] coeffs, int psLowDegree);

    /**
     * compute encrypted query powers.
     *
     * @param encryptionParameters encryption parameters.
     * @param relinKeys            relinearization keys.
     * @param encryptedQuery       encrypted query.
     * @param parentPowers         parent powers.
     * @param sourcePowers         source powers.
     * @param psLowDegree          Paterson-Stockmeyer low degree.
     * @return encrypted query powers.
     */
    static native List<byte[]> computeEncryptedPowers(byte[] encryptionParameters, byte[] relinKeys,
                                                      List<byte[]> encryptedQuery, int[][] parentPowers,
                                                      int[] sourcePowers, int psLowDegree);

    /**
     * Paterson-Stockmeyer compute matches.
     *
     * @param encryptionParameters encryption parameters.
     * @param relinKeys            relinearization keys.
     * @param plaintextPolys       plaintext polynomials.
     * @param ciphertextPolys      ciphertext polynomials.
     * @param psLowDegree          Paterson-Stockmeyer low degree.
     * @return matches.
     */
    static native byte[] optComputeMatches(byte[] encryptionParameters, byte[] relinKeys, List<byte[]> plaintextPolys,
                                           List<byte[]> ciphertextPolys, int psLowDegree);

    /**
     * naive method compute matches.
     *
     * @param encryptionParameters encryption parameters.
     * @param plaintextPolys       plaintext polynomials.
     * @param ciphertextPolys      ciphertext polynomials.
     * @return matches.
     */
    static native byte[] naiveComputeMatches(byte[] encryptionParameters, List<byte[]> plaintextPolys,
                                             List<byte[]> ciphertextPolys);

    /**
     * generate encrypted query.
     *
     * @param encryptionParameters encryption parameters.
     * @param publicKey            public key.
     * @param secretKey            secret key.
     * @param plainQuery           plain query.
     * @return encrypted query.
     */
    static native List<byte[]> generateQuery(byte[] encryptionParameters, byte[] publicKey, byte[] secretKey,
                                             long[][] plainQuery);

    /**
     * decode server response.
     *
     * @param encryptionParameters encryption parameters.
     * @param secretKey            secret key.
     * @param serverResponse       encrypted response.
     * @return decoded response.
     */
    static native long[] decodeReply(byte[] encryptionParameters, byte[] secretKey, byte[] serverResponse);
}
