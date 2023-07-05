package edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir;

import java.util.List;

/**
 * Mul PIR Native utils
 *
 * @author Qixian Zhou
 * @date 2023/5/29
 */
public class Alpr21SingleIndexPirNativeUtils {


    private Alpr21SingleIndexPirNativeUtils() {
        // empty
    }

    /**
     * generate encryption params.
     *
     * @param polyModulusDegree poly modulus degree.
     * @param plainModulus      plain modulus.
     * @return encryption params.
     */
    static native byte[] generateEncryptionParams(int polyModulusDegree, long plainModulus);

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
     * @param galoisKey        Galois keys.
     * @param relinKey         Relin keys
     * @param queryList        query ciphertexts.
     * @param database         database.
     * @param nvec             dimension size.
     * @return response ciphertexts。
     */
    static native List<byte[]> generateReply(byte[] encryptionParams, byte[] galoisKey, byte[] relinKey, List<byte[]> queryList,
                                             byte[][] database, int[] nvec);

    /**
     * decode response.
     *
     * @param encryptionParams SEAL encryption params.
     * @param secretKey        secret key.
     * @param response         response ciphertext.
     * @return BFV plaintext.
     */
    static native long[] decryptReply(byte[] encryptionParams, byte[] secretKey, List<byte[]> response);
}
