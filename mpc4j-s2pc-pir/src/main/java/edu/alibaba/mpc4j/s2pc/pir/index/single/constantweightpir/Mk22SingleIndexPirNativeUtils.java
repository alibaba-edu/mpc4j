package edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir;

import java.util.List;

/**
 * Constant-weight Pir Native Utils
 *
 * @author Qixian Zhou
 * @date 2023/6/18
 */
public class Mk22SingleIndexPirNativeUtils {

    private Mk22SingleIndexPirNativeUtils() {
        // empty
    }

    /**
     * generate encryption params.
     *
     * @param polyModulusDegree poly modulus degree.
     * @param plainModulus      plain modulus
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
     * @param encryptionParams  SEAL encryption params.
     * @param publicKey         public key.
     * @param secretKey         secret key.
     * @param encodedIndex      index after encode: E_q.
     * @param usedSlotsPerPlain 2^c.
     * @param numInputCipher    h.
     * @return query ciphertexts.
     */
    static native List<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey,
                                             int[] encodedIndex, int usedSlotsPerPlain, int numInputCipher);

    /**
     * generate response.
     *
     * @param encryptionParams        params.
     * @param galoisKey               galois keys.
     * @param relinKey                relin keys.
     * @param queryList               query list.
     * @param database                database contains BFV PTS.
     * @param plaintextIndexCodewords server BFV PT index codewords.
     * @param numInputCiphers         h.
     * @param codewordBitLength       k.
     * @param hammingWeight           m.
     * @param eqType                  folklore or constant-weight.
     * @return response ciphertexts.
     */
    static native byte[] generateReply(byte[] encryptionParams, byte[] galoisKey, byte[] relinKey, List<byte[]> queryList,
                                       byte[][] database, List<int[]> plaintextIndexCodewords, int numInputCiphers,
                                       int codewordBitLength, int hammingWeight, int eqType);

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
