package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import java.util.List;

/**
 * OnionPIR native utils.
 *
 * @author Liqiang Peng
 * @date 2022/11/14
 */
class Mcr21IndexPirNativeUtils {

    private Mcr21IndexPirNativeUtils() {
        // empty
    }

    /**
     * generate encryption params.
     *
     * @param modulusDegree    poly modulus degree.
     * @param plainModulusSize plain modulus size.
     * @return encryption params.
     */
    static native byte[] generateEncryptionParams(int modulusDegree, int plainModulusSize);

    /**
     * generate key pair.
     *
     * @param encryptionParams SEAL encryption params.
     * @return key pair.
     */
    static native List<byte[]> keyGen(byte[] encryptionParams);

    /**
     * preprocess database.
     *
     * @param encryptionParams SEAL encryption params.
     * @param plaintextList    plaintexts.
     * @return decomposed plaintexts.
     */
    static native List<long[]> preprocessDatabase(byte[] encryptionParams, List<long[]> plaintextList);

    /**
     * encrypt secret key.
     *
     * @param encryptionParams SEAL encryption params.
     * @param publicKey        public key.
     * @param secretKey        secret key.
     * @return encrypted secret key.
     */
    static native List<byte[]> encryptSecretKey(byte[] encryptionParams, byte[] publicKey, byte[] secretKey);

    /**
     * generate query.
     *
     * @param encryptionParams SEAL encryption params.
     * @param publicKey        public key.
     * @param secretKey        secret key.
     * @param indices          retrieval indices.
     * @param nvec             dimension size.
     * @return query ciphertexts.
     */
    static native List<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey, int[] indices,
                                             int[] nvec);

    /**
     * generate response.
     *
     * @param encryptionParams SEAL encryption params.
     * @param publicKey        public key.
     * @param galoisKey        Galois keys.
     * @param encSecretKey     encrypted secret key.
     * @param queryList        query ciphertexts.
     * @param dbPlaintexts     database plaintexts.
     * @param nvec             dimension size.
     * @return 检索结果密文。
     */
    static native byte[] generateReply(byte[] encryptionParams, byte[] publicKey, byte[] galoisKey,
                                       List<byte[]> encSecretKey, List<byte[]> queryList, List<long[]> dbPlaintexts,
                                       int[] nvec);

    /**
     * decode response.
     *
     * @param encryptionParams SEAL encryption params.
     * @param secretKey        secret key.
     * @param response         response ciphertext.
     * @return BFV plaintext.
     */
    static native long[] decryptReply(byte[] encryptionParams, byte[] secretKey, byte[] response);
}
