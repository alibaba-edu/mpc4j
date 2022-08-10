package edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21;

import java.util.List;

/**
 * CMG21非平衡PSI协议本地客户端。
 *
 * @author Liqiang Peng
 * @date 2022/6/15
 */
class Cmg21UpsiNativeClient {

    private Cmg21UpsiNativeClient() {

    }

    /**
     * 生成加密方案参数和公私钥。
     *
     * @param modulusDegree    多项式阶。
     * @param plainModulus     明文模数。
     * @param coeffModulusBits 系数模数的比特值。
     * @return 加密方案参数和公私钥。
     */
    static native List<byte[]> genEncryptionParameters(int modulusDegree, long plainModulus, int[] coeffModulusBits);

    /**
     * 生成索引信息。
     *
     * @param encryptionParams 加密方案参数。
     * @param publicKey        公钥。
     * @param secretKey        私钥。
     * @param plainQuery       明文索引信息。
     * @return 索引信息。
     */
    static native List<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey, long[][] plainQuery);

    /**
     * 解码查询结果。
     *
     * @param encryptionParams  加密方案参数。
     * @param secretKey         私钥。
     * @param encryptedResponse 密文查询结果。
     * @return 查询结果。
     */
    static native long[] decodeReply(byte[] encryptionParams, byte[] secretKey, byte[] encryptedResponse);
}
