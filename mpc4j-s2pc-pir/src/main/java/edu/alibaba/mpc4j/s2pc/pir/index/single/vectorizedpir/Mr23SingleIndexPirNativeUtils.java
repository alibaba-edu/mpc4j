package edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir;

import java.util.List;

/**
 * Vectorized PIR native utils.
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
class Mr23SingleIndexPirNativeUtils {

    private Mr23SingleIndexPirNativeUtils() {
        // empty
    }

    /**
     * generate encryption params.
     *
     * @param polyModulusDegree     poly modulus degree.
     * @param plainModulusBitLength plain modulus bit length.
     * @return encryption params.
     */
    static native byte[] generateEncryptionParams(int polyModulusDegree, int plainModulusBitLength);

    /**
     * generate key pair.
     *
     * @param encryptionParams encryption params.
     * @return key pair.
     */
    static native List<byte[]> keyGen(byte[] encryptionParams);

    /**
     * database preprocess.
     *
     * @param encryptionParams encryption params.
     * @param coeffs           coeffs.
     * @return BFV plaintexts in NTT form.
     */
    static native List<byte[]> preprocessDatabase(byte[] encryptionParams, long[][] coeffs);

    /**
     * generate query.
     *
     * @param encryptionParams encryption params.
     * @param publicKey        public key.
     * @param secretKey        secret key.
     * @param indices          indices.
     * @return query ciphertexts.
     */
    static native List<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey, int[] indices);

    /**
     * generate response.
     *
     * @param encryptionParams      encryption params.
     * @param queryList             query ciphertext.
     * @param dbPlaintexts          encoded database.
     * @param publicKey             public key.
     * @param relinKeys             relinearization keys.
     * @param galoisKeys            Galois keys.
     * @param firstTwoDimensionSize first two dimension size.
     * @return response ciphertexts。
     */
    static native byte[] generateReply(byte[] encryptionParams, List<byte[]> queryList, byte[][] dbPlaintexts,
                                       byte[] publicKey, byte[] relinKeys, byte[] galoisKeys, int firstTwoDimensionSize);

    /**
     * decode response.
     *
     * @param encryptionParams encryption params.
     * @param secretKey        secret key.
     * @param response         response ciphertext.
     * @param offset           offset.
     * @param gap              gap.
     * @return coefficient.
     */
    static native long decryptReply(byte[] encryptionParams, byte[] secretKey, byte[] response, int offset, int gap);
}
