package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import java.util.List;

/**
 * CMG21关键词索引PIR协议本地服务端。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
class Cmg21KwPirNativeServer {
    /**
     * 单例模式
     */
    private Cmg21KwPirNativeServer() {
        // empty
    }

    /**
     * 计算密文的幂次方。
     *
     * @param encryptionParams 加密方案参数。
     * @param relinKeys        重线性化密钥。
     * @param encryptedQuery   加密的查询信息。
     * @param parentPowers     父幂次方。
     * @param sourcePowers     源幂次方。
     * @param psLowDegree      Paterson-Stockmeyer方法的低阶值。
     * @return 密文的幂次方。
     */
    static native List<byte[]> computeEncryptedPowers(byte[] encryptionParams, byte[] relinKeys,
                                                      List<byte[]> encryptedQuery, int[][] parentPowers,
                                                      int[] sourcePowers, int psLowDegree);


    /**
     * Paterson-Stockmeyer方法计算密文匹配结果。
     *
     * @param encryptionParams 加密方案参数。
     * @param publicKey        公钥。
     * @param relinKeys        重线性化密钥。
     * @param plaintextPolys   明文多项式。
     * @param ciphertextPolys  密文多项式。
     * @param psLowDegree      Paterson-Stockmeyer方法的低阶值。
     * @return 密文匹配结果。
     */
    static native byte[] computeMatches(byte[] encryptionParams, byte[] publicKey, byte[] relinKeys,
                                        long[][] plaintextPolys, List<byte[]> ciphertextPolys, int psLowDegree);

    /**
     * 一般方法计算密文匹配结果。
     *
     * @param encryptionParams 加密方案参数。
     * @param publicKey        公钥。
     * @param relinKeys        重线性化密钥。
     * @param plaintextPolys   数据库编码。
     * @param ciphertextPolys  密文查询信息。
     * @return 密文匹配结果。
     */
    static native byte[] computeMatchesNaiveMethod(byte[] encryptionParams, byte[] publicKey, byte[] relinKeys,
                                                   long[][] plaintextPolys, List<byte[]> ciphertextPolys);

}
