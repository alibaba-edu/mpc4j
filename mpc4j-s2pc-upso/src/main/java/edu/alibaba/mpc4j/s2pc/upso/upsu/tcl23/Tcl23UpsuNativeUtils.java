package edu.alibaba.mpc4j.s2pc.upso.upsu.tcl23;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.util.List;

/**
 * TCL23 UPSU native utils.
 *
 * @author Liqiang Peng
 * @date 2024/3/8
 */
public class Tcl23UpsuNativeUtils {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    private Tcl23UpsuNativeUtils() {
        // empty
    }

    /**
     * generate encryption params.
     *
     * @param polyModulusDegree poly modulus degree.
     * @param plainModulus      plain modulus.
     * @param coeffModulusBits  coeffs modulus bit.
     * @return encryption parameters.
     */
    static native byte[] genEncryptionParameters(int polyModulusDegree, long plainModulus, int[] coeffModulusBits);

    /**
     * key generation.
     *
     * @param encryptionParameters encryption parameters.
     * @return key pair.
     */
    static native List<byte[]> keyGen(byte[] encryptionParameters);

    /**
     * preprocess database.
     *
     * @param encryptionParameters encryption parameters.
     * @param coeffs               plaintexts in coefficient form.
     * @param psLowDegree          Paterson-Stockmeyer low degree.
     * @return plaintexts in NTT form.
     */
    static native List<byte[]> preprocessDatabase(byte[] encryptionParameters, long[][] coeffs, int psLowDegree);

    /**
     * compute all powers of the query.
     *
     * @param encryptionParams encryption parameters.
     * @param relinKeys        relinearization keys.
     * @param encryptedQuery   encrypted query.
     * @param parentPowers     parent power.
     * @param sourcePowers     source powers.
     * @param psLowDegree      Paterson-Stockmeyer low degree.
     * @return encrypted query powers.
     */
    static native List<byte[]> computeEncryptedPowers(byte[] encryptionParams, byte[] relinKeys,
                                                      List<byte[]> encryptedQuery, int[][] parentPowers,
                                                      int[] sourcePowers, int psLowDegree);

    /**
     * evaluate the polynomial on the given ciphertext using the Paterson-Stockmeyer algorithm.
     *
     * @param encryptionParams encryption parameters.
     * @param relinKeys        relinearization keys.
     * @param plaintexts       plaintexts.
     * @param ciphertexts      ciphertexts.
     * @param psLowDegree      Paterson-Stockmeyer low degree.
     * @param mask             random mask.
     * @return encrypted matches.
     */
    static native byte[] optComputeMatches(byte[] encryptionParams, byte[] relinKeys, List<byte[]> plaintexts,
                                           List<byte[]> ciphertexts, int psLowDegree, long[] mask);

    /**
     * evaluate the polynomial on the given ciphertext using the naive algorithm.
     *
     * @param encryptionParams encryption parameters.
     * @param plaintexts       plaintexts.
     * @param ciphertexts      ciphertexts.
     * @param mask             random mask.
     * @return encrypted matches.
     */
    static native byte[] naiveComputeMatches(byte[] encryptionParams, List<byte[]> plaintexts, List<byte[]> ciphertexts,
                                             long[] mask);

    /**
     * encrypt query.
     *
     * @param encryptionParams encryption parameters.
     * @param secretKey        secret key.
     * @param plainQuery       query in plaintext.
     * @return encrypted query.
     */
    static native List<byte[]> generateQuery(byte[] encryptionParams, byte[] secretKey, long[][] plainQuery);

    /**
     * decrypt response.
     *
     * @param encryptionParams  encryption parameters.
     * @param secretKey         secret key.
     * @param encryptedResponse response.
     * @return plaintext in coefficient form.
     */
    static native long[] decodeReply(byte[] encryptionParams, byte[] secretKey, byte[] encryptedResponse);
}
