package edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir;

import java.util.List;

/**
 * Vectorized Batch PIR native utils.
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
class Mr23BatchIndexPirNativeUtils {

    private Mr23BatchIndexPirNativeUtils() {
        // empty
    }

    /**
     * generate encryption parameters.
     *
     * @param polyModulusDegree     poly modulus degree.
     * @param plainModulusBitLength plain modulus bit length.
     * @return encryption parameters.
     */
    static native byte[] generateEncryptionParams(int polyModulusDegree, int plainModulusBitLength);

    /**
     * key generations.
     *
     * @param encryptionParameters encryption parameters.
     * @return key pair.
     */
    static native List<byte[]> keyGen(byte[] encryptionParameters);

    /**
     * preprocess database.
     *
     * @param encryptionParameters encryption parameters.
     * @param coeffs               coefficients.
     * @param totalSize            total plaintext size.
     * @return plaintext in NTT form.
     */
    static native List<byte[]> preprocessDatabase(byte[] encryptionParameters, long[][] coeffs, int totalSize);

    /**
     * generate encrypted query.
     *
     * @param encryptionParameters encryption parameters.
     * @param publicKey            public key.
     * @param secretKey            secret key.
     * @param queries              plain query.
     * @return encrypted query.
     */
    static native List<byte[]> generateQuery(byte[] encryptionParameters, byte[] publicKey, byte[] secretKey,
                                             long[][] queries);


    /**
     * generate response.
     *
     * @param encryptionParameters  encryption parameters.
     * @param queryList             query list.
     * @param dbPlaintexts          database plaintext.
     * @param publicKey             public key.
     * @param relinKeys             relinearization keys.
     * @param galoisKeys            Galois keys.
     * @param firstTwoDimensionSize first two dimension size.
     * @return response.
     */
    static native byte[] generateReply(byte[] encryptionParameters, List<byte[]> queryList, List<byte[]> dbPlaintexts,
                                       byte[] publicKey, byte[] relinKeys, byte[] galoisKeys, int firstTwoDimensionSize);

    /**
     * decrypt reply.
     *
     * @param encryptionParameters encryption parameters.
     * @param secretKey            secret key.
     * @param response             response.
     * @return retrieval result.
     */
    static native long[] decryptReply(byte[] encryptionParameters, byte[] secretKey, byte[] response);

    /**
     * merge responses.
     *
     * @param encryptionParameters encryption parameters.
     * @param galoisKey            Galois keys.
     * @param responses            responses.
     * @param g                    vectorized batch PIR params.
     * @return merged reponse.
     */
    static native byte[] mergeResponse(byte[] encryptionParameters, byte[] galoisKey, List<byte[]> responses, int g);
}
