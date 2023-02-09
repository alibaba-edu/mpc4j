package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import java.util.ArrayList;
import java.util.List;

/**
 * OnionPIR协议本地算法库工具类。
 *
 * @author Liqiang Peng
 * @date 2022/11/14
 */
class Mcr21IndexPirNativeUtils {
    /**
     * 单例模式
     */
    private Mcr21IndexPirNativeUtils() {
        // empty
    }

    /**
     * 生成SEAL上下文参数。
     *
     * @param modulusDegree          多项式阶。
     * @param plainModulusBitLength  明文模数比特长度。
     * @return SEAL上下文参数。
     */
    static native byte[] generateSealContext(int modulusDegree, int plainModulusBitLength);

    /**
     * 生成全同态加密密钥对。
     *
     * @param sealContext SEAL上下文参数。
     * @return 公私钥对。
     */
    static native ArrayList<byte[]> keyGen(byte[] sealContext);

    /**
     * 数据库预处理。
     *
     * @param sealContext SEAL上下文参数。
     * @param plaintext   明文。
     * @return 拆分后的明文多项式。
     */
    static native ArrayList<long[]> preprocessDatabase(byte[] sealContext, List<long[]> plaintext);

    /**
     * 加密私钥。
     *
     * @param sealContext SEAL上下文参数
     * @param publicKey   公钥。
     * @param secretKey   私钥。
     * @return 私钥密文。
     */
    static native ArrayList<byte[]> encryptSecretKey(byte[] sealContext, byte[] publicKey, byte[] secretKey);

    /**
     * 生成问询密文。
     *
     * @param sealContext SEAL上下文参数。
     * @param publicKey   公钥。
     * @param secretKey   私钥。
     * @param indices     索引值。
     * @param nvec        各维度向量长度。
     * @return 问询密文。
     */
    static native ArrayList<byte[]> generateQuery(byte[] sealContext, byte[] publicKey, byte[] secretKey, int[] indices,
                                                  int[] nvec);

    /**
     * 生成回复密文。
     *
     * @param sealContext  SEAL上下文参数。
     * @param publicKey    公钥。
     * @param galoisKey    Galois密钥。
     * @param encSecretKey 私钥密文。
     * @param queryList    检索值密文。
     * @param dbPlaintexts 数据库明文。
     * @param nvec         各维度向量长度。
     * @return 检索结果密文。
     */
    static native byte[] generateReply(byte[] sealContext, byte[] publicKey, byte[] galoisKey, List<byte[]> encSecretKey,
                                       List<byte[]> queryList, List<long[]> dbPlaintexts, int[] nvec);

    /**
     * 解密回复密文。
     *
     * @param sealContext SEAL上下文参数。
     * @param secretKey   私钥。
     * @param response    回复密文。
     * @return 查询结果。
     */
    static native long[] decryptReply(byte[] sealContext, byte[] secretKey, byte[] response);
}
