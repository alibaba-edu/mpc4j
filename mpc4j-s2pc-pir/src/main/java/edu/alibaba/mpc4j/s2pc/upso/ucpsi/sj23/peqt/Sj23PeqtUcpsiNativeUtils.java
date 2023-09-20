package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.util.List;

/**
 * @author Liqiang Peng
 * @date 2023/7/17
 */
public class Sj23PeqtUcpsiNativeUtils {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    private Sj23PeqtUcpsiNativeUtils() {
        // empty
    }

    /**
     * generate encryption params.
     *
     * @param polyModulusDegree poly modulus degree.
     * @param plainModulus      plain modulus.
     * @param coeffModulusBits  coeff modulus bits.
     * @return encryption params.
     */
    static native byte[] genEncryptionParameters(int polyModulusDegree, long plainModulus, int[] coeffModulusBits);

    /**
     * generate key pair.
     *
     * @param encryptionParams encryption params.
     * @return key pair.
     */
    static native List<byte[]> keyGen(byte[] encryptionParams);

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
    static native byte[] optComputeMatches(byte[] encryptionParams, byte[] publicKey, byte[] relinKeys, long[][] plaintextPolys,
                                           List<byte[]> ciphertextPolys, int psLowDegree, long[] mask);

    /**
     * naive method compute matches.
     *
     * @param encryptionParams encryption params.
     * @param plaintextPolys   plaintexts.
     * @param ciphertextPolys  ciphertexts.
     * @return encrypted matches.
     */
    static native byte[] naiveComputeMatches(byte[] encryptionParams, byte[] publicKey, long[][] plaintextPolys,
                                             List<byte[]> ciphertextPolys, long[] mask);

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
