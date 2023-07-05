package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.util.List;

/**
 * CMG21 UPSI native utils.
 *
 * @author Liqiang Peng
 * @date 2022/11/5
 */
public class Cmg21UpsiNativeUtils {
    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    private Cmg21UpsiNativeUtils() {
        // empty
    }

    /**
     * generate encryption params.
     *
     * @param polyModulusDegree poly modulus degree.
     * @param plainModulus      plain modulus.
     * @param coeffModulusBits  coeffs modulus bit.
     * @return encryption params.
     */
    static native List<byte[]> genEncryptionParameters(int polyModulusDegree, long plainModulus, int[] coeffModulusBits);

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
    static native List<byte[]> computeEncryptedPowers(byte[] encryptionParams, byte[] relinKeys,
                                                      List<byte[]> encryptedQuery, int[][] parentPowers,
                                                      int[] sourcePowers, int psLowDegree);

    /**
     * Paterson-Stockmeyer compute matches.
     *
     * @param encryptionParams encryption params.
     * @param relinKeys        relinearization keys.
     * @param plaintextPolys   plaintexts.
     * @param ciphertextPolys  ciphertexts.
     * @param psLowDegree      Paterson-Stockmeyer low degree.
     * @return encrypted matches.
     */
    static native byte[] optComputeMatches(byte[] encryptionParams, byte[] relinKeys, long[][] plaintextPolys,
                                           List<byte[]> ciphertextPolys, int psLowDegree);

    /**
     * naive method compute matches.
     *
     * @param encryptionParams encryption params.
     * @param plaintextPolys   plaintexts.
     * @param ciphertextPolys  ciphertexts.
     * @return encrypted matches.
     */
    static native byte[] naiveComputeMatches(byte[] encryptionParams, long[][] plaintextPolys,
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
    static native List<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey,
                                             long[][] plainQuery);

    /**
     * decode server response.
     *
     * @param encryptedResponse server response.
     * @param encryptionParams  encryption params.
     * @param secretKey         secret key.
     * @return retrieval result.
     */
    static native long[] decodeReply(byte[] encryptionParams, byte[] secretKey, byte[] encryptedResponse);
}
