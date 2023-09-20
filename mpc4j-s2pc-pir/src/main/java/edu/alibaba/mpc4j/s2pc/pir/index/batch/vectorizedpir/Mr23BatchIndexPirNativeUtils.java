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
     * @return plaintext in NTT form.
     */
    static native List<byte[]> preprocessDatabase(byte[] encryptionParameters, long[][] coeffs);

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
     * @param thirdDimensionSize    third dimension size.
     * @param partitionSize         partition size.
     * @return response.
     */
    static native List<byte[]> generateReply(byte[] encryptionParameters, List<byte[]> queryList, List<byte[]> dbPlaintexts,
                                             byte[] publicKey, byte[] relinKeys, byte[] galoisKeys, int firstTwoDimensionSize,
                                             int thirdDimensionSize, int partitionSize);

    /**
     * decrypt reply.
     *
     * @param encryptionParameters encryption parameters.
     * @param secretKey            secret key.
     * @param response             response.
     * @return retrieval result.
     */
    static native long[] decryptReply(byte[] encryptionParameters, byte[] secretKey, byte[] response);
}
