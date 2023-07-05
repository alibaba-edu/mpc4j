package edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.util.List;

/**
 * AAAG22 keyword native utils.
 *
 * @author Liqiang Peng
 * @date 2023/6/16
 */
class Aaag22KwPirNativeUtils {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    private Aaag22KwPirNativeUtils() {
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
    static native byte[] genEncryptionParameters(int polyModulusDegree, long plainModulus, int[] coeffModulusBits);

    /**
     * generate key pair.
     *
     * @param encryptionParams encryption params.
     * @return key pair.
     */
    static native List<byte[]> keyGen(byte[] encryptionParams, int pirColumnNumPerObj, int colNum);

    /**
     * transform polynomials into NTT form.
     *
     * @param encryptionParams encryption params.
     * @param coeffs           polynomial coeffs.
     * @return polynomials in NTT form.
     */
    static native List<byte[]> nttTransform(byte[] encryptionParams, long[][] coeffs);

    /**
     * preprocess masks.
     *
     * @param encryptionParams encryption params.
     * @param colNum           column num.
     * @return masks.
     */
    static native List<byte[]> preprocessMask(byte[] encryptionParams, int colNum);

    /**
     * generate query.
     *
     * @param encryptionParams encryption params.
     * @param publicKey        public key.
     * @param secretKey        secret key.
     * @param query            query.
     * @return client query.
     */
    static native byte[] generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey, long[] query);

    /**
     * server expand query.
     *
     * @param encryptionParams encryption params.
     * @param galoisKeys       Galois keys.
     * @param masks            masks.
     * @param query            client query.
     * @param colNum           column.
     * @return expanded queries.
     */
    static native List<byte[]> expandQuery(byte[] encryptionParams, byte[] galoisKeys, List<byte[]> masks, byte[] query,
                                           int colNum);

    /**
     * server process column.
     *
     * @param encryptionParams encryption params.
     * @param publicKey        public key.
     * @param relinKeys        relin keys.
     * @param pt               plaintext.
     * @param ct               ciphertext.
     * @return column result.
     */
    static native byte[] processColumn(byte[] encryptionParams, byte[] publicKey, byte[] relinKeys, long[] pt, byte[] ct);

    /**
     * server process row.
     *
     * @param encryptionParams encryption params.
     * @param relinKeys        relin keys.
     * @param galoisKeys       Galois keys.
     * @param columnResults    column results.
     * @return row result.
     */
    static native byte[] processRow(byte[] encryptionParams, byte[] relinKeys, byte[] galoisKeys,
                                    List<byte[]> columnResults);

    /**
     * server process index PIR.
     *
     * @param encryptionParams encryption params.
     * @param galoisKeys       Galois keys.
     * @param encodedLabel     encoded label.
     * @param rowResults       row results.
     * @param columnNumPerObj  column num per label.
     * @return retrieval label.
     */
    static native byte[] processPir(byte[] encryptionParams, byte[] galoisKeys, List<byte[]> encodedLabel, List<byte[]>
                                    rowResults, int columnNumPerObj);

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
