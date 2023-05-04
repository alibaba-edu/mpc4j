package edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir;

import java.util.ArrayList;
import java.util.List;

/**
 * Vectorized PIR协议本地算法库工具类。
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
class Mr23IndexPirNativeUtils {

    /**
     * 单例模式
     */
    private Mr23IndexPirNativeUtils() {
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
     * 生成全同态加密公私钥对。
     *
     * @param sealContext SEAL上下文参数。
     * @param slotNum     卡槽数目。
     * @return 公私钥对。
     */
    static native List<byte[]> keyGen(byte[] sealContext, int slotNum);

    /**
     * 数据库预处理。
     *
     * @param sealContext    SEAL上下文参数。
     * @param db             数据库数据。
     * @param dimensionsSize 维度长度。
     * @param totalSize      多项式数量。
     * @return 明文多项式。
     */
    static native ArrayList<byte[]> preprocessDatabase(byte[] sealContext, long[] db, int[] dimensionsSize,
                                                       int totalSize);

    /**
     * 生成问询密文。
     *
     * @param sealContext SEAL上下文参数。
     * @param publicKey   公钥。
     * @param secretKey   私钥。
     * @param indices     明文检索值。
     * @param slotNum     卡槽数目。
     * @return 问询密文。
     */
    static native ArrayList<byte[]> generateQuery(byte[] sealContext, byte[] publicKey, byte[] secretKey, int[] indices,
                                                  int slotNum);

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
     * @param thirdDimensionSize    第三维向量长度。
     * @return 检索结果密文。
     */
    static native byte[] generateReply(byte[] sealContext, List<byte[]> queryList, List<byte[]> dbPlaintexts,
                                       byte[] publicKey, byte[] relinKeys, byte[] galoisKeys, int firstTwoDimensionSize,
                                       int thirdDimensionSize);

    /**
     * 解密回复密文。
     *
     * @param sealContext SEAL上下文参数。
     * @param secretKey   私钥。
     * @param response    回复密文。
     * @param offset      移位。
     * @param slotNum     卡槽数目。
     * @return 查询结果。
     */
    static native long decryptReply(byte[] sealContext, byte[] secretKey, byte[] response, int offset, int slotNum);
}
