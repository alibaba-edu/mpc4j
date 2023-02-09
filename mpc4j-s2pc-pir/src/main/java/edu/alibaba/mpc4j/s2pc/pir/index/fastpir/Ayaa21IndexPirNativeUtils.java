package edu.alibaba.mpc4j.s2pc.pir.index.fastpir;

import java.util.ArrayList;

/**
 * FastPIR协议本地算法库工具类。
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class Ayaa21IndexPirNativeUtils {

    /**
     * 单例模式
     */
    private Ayaa21IndexPirNativeUtils() {
        // empty
    }

    /**
     * 生成SEAL上下文参数。
     *
     * @param modulusDegree 多项式阶。
     * @param plainModulus  明文模数。
     * @param coeffModulus  密文模数。
     * @return SEAL上下文参数。
     */
    static native byte[] generateSealContext(int modulusDegree, long plainModulus, long[] coeffModulus);

    /**
     * 生成全同态加密密钥对。
     *
     * @param sealContext SEAL上下文参数。
     * @param steps       Galois密钥参数。
     * @return 公私钥对。
     */
    static native ArrayList<byte[]> keyGen(byte[] sealContext, int[] steps);

    /**
     * NTT转换。
     *
     * @param sealContext SEAL上下文参数。
     * @param plaintext   系数表示的多项式。
     * @return 点值表示的多项式。
     */
    static native ArrayList<byte[]> nttTransform(byte[] sealContext, long[][] plaintext);

    /**
     * 客户端生成问询密文。
     *
     * @param sealContext SEAL上下文参数
     * @param publicKey   公钥。
     * @param secretKey   私钥。
     * @param index       索引值。
     * @param querySize   问询密文数目。
     * @return 问询密文。
     */
    static native ArrayList<byte[]> generateQuery(byte[] sealContext, byte[] publicKey, byte[] secretKey, int index,
                                                  int querySize);

    /**
     * 服务端生成回复密文。
     *
     * @param sealContext   SEAL上下文参数
     * @param galoisKeys    Galois密钥。
     * @param query         问询密文。
     * @param database      数据库明文。
     * @param elementColNum 元素列数目。
     * @return 检索结果密文。
     */
    static native byte[] generateResponse(byte[] sealContext, byte[] galoisKeys, ArrayList<byte[]> query,
                                          ArrayList<byte[]> database, int elementColNum);

    /**
     * 解密回复密文。
     *
     * @param sealContext SEAL上下文参数。
     * @param secretKey   私钥。
     * @param response    回复密文。
     * @return 查询结果。
     */
    static native long[] decodeResponse(byte[] sealContext, byte[] secretKey, byte[] response);
}
