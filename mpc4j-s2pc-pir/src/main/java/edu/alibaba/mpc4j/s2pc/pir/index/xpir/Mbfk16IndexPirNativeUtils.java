package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import java.util.List;

/**
 * XPIR native utils.
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
class Mbfk16IndexPirNativeUtils {

    private Mbfk16IndexPirNativeUtils() {
        // empty
    }

    /**
     * generate encryption params.
     *
     * @param modulusDegree poly modulus degree.
     * @param plainModulus  plain modulus.
     * @return encryption params.
     */
    static native byte[] generateEncryptionParams(int modulusDegree, long plainModulus);

    /**
     * generate key pair.
     *
     * @param encryptionParams SEAL encryption params.
     * @return key pair.
     */
    static native List<byte[]> keyGen(byte[] encryptionParams);

    /**
     * NTT transformation.
     *
     * @param encryptionParams SEAL encryption params.
     * @param plaintextList    BFV plaintexts not in NTT form.
     * @return BFV plaintexts in NTT form.
     */
    static native List<byte[]> nttTransform(byte[] encryptionParams, List<long[]> plaintextList);

    /**
     * generate query.
     *
     * @param encryptionParams SEAL encryption params.
     * @param publicKey        public key.
     * @param secretKey        secret key.
     * @param indices          indices.
     * @param nvec             dimension size.
     * @return query ciphertexts.
     */
    static native List<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey, int[] indices,
                                             int[] nvec);

    /**
     * generate response.
     *
     * @param encryptionParams SEAL encryption params.
     * @param queryList        query ciphertexts.
     * @param database         database.
     * @param nvec             dimension size.
     * @return response ciphertexts。
     */
    static native List<byte[]> generateReply(byte[] encryptionParams, List<byte[]> queryList, List<byte[]> database,
                                             int[] nvec);

    /**
     * decode response.
     *
     * @param encryptionParams SEAL encryption params.
     * @param secretKey        secret key.
     * @param response         response ciphertext.
     * @param dimension        dimension.
     * @return BFV plaintext.
     */
    static native long[] decryptReply(byte[] encryptionParams, byte[] secretKey, List<byte[]> response, int dimension);

    /**
     * compute size ratio between a ciphertext and the largest plaintext that can be encrypted.
     *
     * @param encryptionParams SEAL encryption params.
     * @return expansion ratio.
     */
    static native int expansionRatio(byte[] encryptionParams);
}
