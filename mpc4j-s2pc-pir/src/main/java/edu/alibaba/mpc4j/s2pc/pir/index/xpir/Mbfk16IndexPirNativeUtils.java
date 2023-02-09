package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import java.util.ArrayList;
import java.util.List;

/**
 * XPIR协议本地算法库工具类。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
class Mbfk16IndexPirNativeUtils {

    /**
     * 单例模式
     */
    private Mbfk16IndexPirNativeUtils() {
        // empty
    }

    /**
     * 生成SEAL上下文参数。
     *
     * @param modulusDegree 多项式阶。
     * @param plainModulus  明文模数。
     * @return SEAL上下文参数。
     */
    static native byte[] generateSealContext(int modulusDegree, long plainModulus);

    /**
     * 生成全同态加密公私钥对。
     *
     * @param sealContext SEAL上下文参数。
     * @return 公私钥对。
     */
    static native List<byte[]> keyGen(byte[] sealContext);

    /**
     * NTT转换。
     *
     * @param sealContext SEAL上下文参数。
     * @param plaintext   系数表示的多项式。
     * @return 点值表示的多项式。
     */
    static native ArrayList<byte[]> nttTransform(byte[] sealContext, List<long[]> plaintext);

    /**
     * 生成问询密文。
     *
     * @param sealContext SEAL上下文参数。
     * @param publicKey   公钥。
     * @param secretKey   私钥。
     * @param indices     明文检索值。
     * @param nvec        各维度向量长度。
     * @return 问询密文。
     */
    static native ArrayList<byte[]> generateQuery(byte[] sealContext, byte[] publicKey, byte[] secretKey, int[] indices,
                                                  int[] nvec);

    /**
     * 生成回复密文。
     *
     * @param sealContext  SEAL上下文参数。
     * @param queryList    检索值密文。
     * @param dbPlaintexts 数据库明文。
     * @param nvec         各维度长度。
     * @return 检索结果密文。
     */
    static native ArrayList<byte[]> generateReply(byte[] sealContext, List<byte[]> queryList, List<byte[]> dbPlaintexts,
                                                  int[] nvec);

    /**
     * 解密回复密文。
     *
     * @param sealContext SEAL上下文参数。
     * @param secretKey   私钥。
     * @param response    回复密文。
     * @param dimension   维度。
     * @return 查询结果。
     */
    static native long[] decryptReply(byte[] sealContext, byte[] secretKey, List<byte[]> response, int dimension);

    /**
     * 返回密文和明文的比例。
     *
     * @param sealContext SEAL上下文参数。
     * @return 密文和明文的比例。
     */
    static native int expansionRatio(byte[] sealContext);
}
