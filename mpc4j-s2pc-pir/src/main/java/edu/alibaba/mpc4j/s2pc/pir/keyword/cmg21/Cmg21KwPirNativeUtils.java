package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.util.List;

/**
 * CMG21 keyword native utils.
 *
 * @author Liqiang Peng
 * @date 2022/11/4
 */
public class Cmg21KwPirNativeUtils {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    private Cmg21KwPirNativeUtils() {
        // empty
    }

    /**
     * generate encryption params.
     *
     * @param polyModulusDegree poly modulus degree.
     * @param plainModulus      plain modulus.
     * @param coeffModulusBits  coeffs modulus bits.
     * @return encryption params.
     */
    public static native byte[] genEncryptionParameters(int polyModulusDegree, long plainModulus, int[] coeffModulusBits);

    /**
     * generate keys.
     *
     * @param encryptionParams encryption params.
     * @return key pair.
     */
    public static native List<byte[]> keyGen(byte[] encryptionParams);

    /**
     * preprocess database.
     *
     * @param encryptionParameters encryption parameters.
     * @param coeffs               coefficients.
     * @param psLowDegree          Paterson-Stockmeyer low degree.
     * @return plaintext in NTT form.
     */
    public static native List<byte[]> preprocessDatabase(byte[] encryptionParameters, long[][] coeffs, int psLowDegree);

    /**
     * check the validity of encryption params.
     *
     * @param polyModulusDegree poly modulus degree.
     * @param plainModulus      plain modulus.
     * @param coeffModulusBits  coeffs modulus bits
     * @param parentPowers      parent powers.
     * @param sourcePowers      source powers.
     * @param psLowDegree       Paterson-Stockmeyer low degree.
     * @param maxBinSize        max bin size.
     * @return whether the encryption params is valid.
     */
    static native boolean checkSealParams(int polyModulusDegree, long plainModulus, int[] coeffModulusBits,
                                          int[][] parentPowers, int[] sourcePowers, int psLowDegree, int maxBinSize);

    /**
     * compute encrypted query powers.
     *
     * @param encryptionParams encryption params.
     * @param relinKeys        relinearization keys.
     * @param encryptedQuery   encrypted query.
     * @param parentPowers     parent power.
     * @param sourcePowers     source powers.
     * @param psLowDegree      Paterson-Stockmeyer low degree.
     * @return encrypted query powers.
     */
    public static native List<byte[]> computeEncryptedPowers(byte[] encryptionParams, byte[] relinKeys,
                                                             List<byte[]> encryptedQuery, int[][] parentPowers,
                                                             int[] sourcePowers, int psLowDegree);

    /**
     * Paterson-Stockmeyer compute matches.
     *
     * @param encryptionParams encryption params.
     * @param publicKey        public key.
     * @param relinKeys        relinearization keys.
     * @param plaintextPolys   plaintexts.
     * @param ciphertextPolys  ciphertexts.
     * @param psLowDegree      Paterson-Stockmeyer low degree.
     * @return encrypted matches.
     */
    public static native byte[] optComputeMatches(byte[] encryptionParams, byte[] publicKey, byte[] relinKeys,
                                                  List<byte[]> plaintextPolys, List<byte[]> ciphertextPolys, int psLowDegree);

    /**
     * naive method compute matches.
     *
     * @param encryptionParams encryption params.
     * @param publicKey        public key.
     * @param plaintextPolys   plaintexts.
     * @param ciphertextPolys  ciphertexts.
     * @return encrypted matches.
     */
    public static native byte[] naiveComputeMatches(byte[] encryptionParams, byte[] publicKey, List<byte[]> plaintextPolys,
                                                    List<byte[]> ciphertextPolys);

    /**
     * generate query.
     *
     * @param encryptionParams encryption params.
     * @param publicKey        public key.
     * @param secretKey        secret key.
     * @param plainQuery       plain query.
     * @return client query.
     */
    public static native List<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey,
                                                    long[][] plainQuery);

    /**
     * decode server response.
     *
     * @param encryptedResponse server response.
     * @param encryptionParams  encryption params.
     * @param secretKey         secret key.
     * @return retrieval result.
     */
    public static native long[] decodeReply(byte[] encryptionParams, byte[] secretKey, byte[] encryptedResponse);
}
