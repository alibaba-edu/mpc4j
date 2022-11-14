package edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.util.List;

/**
 * CMG21非平衡PSI协议本地算法库工具类。
 *
 * @author Liqiang Peng
 * @date 2022/11/5
 */
class Cmg21UpsiNativeUtils {
    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    private Cmg21UpsiNativeUtils() {
        // empty
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
     * 检查SEAL参数是否有效。
     *
     * @param modulusDegree    多项式阶。
     * @param plainModulus     明文模数。
     * @param coeffModulusBits 系数模数的比特值。
     * @param parentPowers     父幂次方。
     * @param sourcePowers     源幂次方。
     * @param psLowDegree      Paterson-Stockmeyer方法的低阶值。
     * @param maxBinSize       每个哈希桶内分块的最大元素个数。
     * @return SEAL参数是否有效。
     */
    static native boolean checkSealParams(int modulusDegree, long plainModulus, int[] coeffModulusBits,
                                          int[][] parentPowers, int[] sourcePowers, int psLowDegree, int maxBinSize);

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
     * @param relinKeys        重线性化密钥。
     * @param plaintextPolys   明文多项式。
     * @param ciphertextPolys  密文多项式。
     * @param psLowDegree      Paterson-Stockmeyer方法的低阶值。
     * @return 密文匹配结果。
     */
    static native byte[] optComputeMatches(byte[] encryptionParams, byte[] relinKeys, long[][] plaintextPolys,
                                        List<byte[]> ciphertextPolys, int psLowDegree);

    /**
     * 一般方法计算密文匹配结果。
     *
     * @param encryptionParams 加密方案参数。
     * @param plaintextPolys   数据库编码。
     * @param ciphertextPolys  密文查询信息。
     * @return 密文匹配结果。
     */
    static native byte[] naiveComputeMatches(byte[] encryptionParams, long[][] plaintextPolys, List<byte[]> ciphertextPolys);

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
