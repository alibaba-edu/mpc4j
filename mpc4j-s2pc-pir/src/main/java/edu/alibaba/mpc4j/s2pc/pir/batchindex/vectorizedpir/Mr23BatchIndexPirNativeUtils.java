package edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir;

import java.util.List;

/**
 * Vectorized Batch PIR协议本地算法库工具类。
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
class Mr23BatchIndexPirNativeUtils {

    private Mr23BatchIndexPirNativeUtils() {
        // empty
    }

    /**
     * 生成SEAL上下文参数。
     *
     * @param modulusDegree         多项式阶。
     * @param plainModulusBitLength 明文模数比特长度。
     * @return SEAL上下文参数。
     */
    static native byte[] generateSealContext(int modulusDegree, int plainModulusBitLength);

    /**
     * 生成全同态加密公私钥对。
     *
     * @param sealContext   SEAL上下文参数。
     * @param dimensionSize 维度长度。
     * @return 公私钥对。
     */
    static native List<byte[]> keyGen(byte[] sealContext, int dimensionSize);

    /**
     * 数据库预处理。
     *
     * @param sealContext SEAL上下文参数。
     * @param coeffs      多项式系数。
     * @param totalSize   多项式数目。
     * @return 明文多项式。
     */
    static native List<byte[]> preprocessDatabase(byte[] sealContext, long[][] coeffs, int totalSize);

    /**
     * 生成问询密文。
     *
     * @param sealContext SEAL上下文参数。
     * @param publicKey   公钥。
     * @param secretKey   私钥。
     * @param queries     明文检索信息。
     * @return 问询密文。
     */
    static native List<byte[]> generateQuery(byte[] sealContext, byte[] publicKey, byte[] secretKey, long[][] queries);


    /**
     * 生成回复密文。
     *
     * @param sealContext           SEAL上下文参数。
     * @param queryList             检索值密文。
     * @param dbPlaintexts          数据库明文。
     * @param publicKey             公钥。
     * @param relinKeys             重线性化密钥。
     * @param galoisKeys            Galois密钥。
     * @param firstTwoDimensionSize 前两维向量长度。
     * @return 检索结果密文。
     */
    static native byte[] generateReply(byte[] sealContext, List<byte[]> queryList, List<byte[]> dbPlaintexts,
                                       byte[] publicKey, byte[] relinKeys, byte[] galoisKeys, int firstTwoDimensionSize);

    /**
     * 解密回复密文。
     *
     * @param sealContext SEAL上下文参数。
     * @param secretKey   私钥。
     * @param response    回复密文。
     * @return 查询结果。
     */
    static native long[] decryptReply(byte[] sealContext, byte[] secretKey, byte[] response);

    /**
     * 合并多个分桶的回复密文。
     *
     * @param sealContext SEAL上下文参数。
     * @param galoisKey   Galois密钥。
     * @param responses   回复密文。
     * @param g           vectorized batch pir 参数。
     * @return 回复密文。
     */
    static native byte[] mergeResponse(byte[] sealContext, byte[] galoisKey, List<byte[]> responses, int g);
}
