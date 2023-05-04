package edu.alibaba.mpc4j.s2pc.pir.batchindex.psipir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.util.List;

/**
 * PSI-PIR协议本地算法库工具类。
 *
 * @author Liqiang Peng
 * @date 2023/3/9
 */
public class Lpzg24BatchIndexPirNativeUtils {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    private Lpzg24BatchIndexPirNativeUtils() {
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
     * 数据库预处理。
     *
     * @param sealContext SEAL上下文参数。
     * @param coeffs      系数。
     * @param psLowDegree Paterson-Stockmeyer方法的低阶值。
     * @return 序列化后的多项式。
     */
    static native List<byte[]> processDatabase(byte[] sealContext, long[][] coeffs, int psLowDegree);

    /**
     * 计算密文的幂次方。
     *
     * @param sealContext    SEAL上下文参数。
     * @param relinKeys      重线性化密钥。
     * @param encryptedQuery 加密的查询信息。
     * @param parentPowers   父幂次方。
     * @param sourcePowers   源幂次方。
     * @param psLowDegree    Paterson-Stockmeyer方法的低阶值。
     * @return 密文的幂次方。
     */
    static native List<byte[]> computeEncryptedPowers(byte[] sealContext, byte[] relinKeys, List<byte[]> encryptedQuery,
                                                      int[][] parentPowers, int[] sourcePowers, int psLowDegree);

    /**
     * Paterson-Stockmeyer方法计算密文匹配结果。
     *
     * @param sealContext     SEAL上下文参数。
     * @param relinKeys       重线性化密钥。
     * @param plaintextPolys  明文多项式。
     * @param ciphertextPolys 密文多项式。
     * @param psLowDegree     Paterson-Stockmeyer方法的低阶值。
     * @return 密文匹配结果。
     */
    static native byte[] optComputeMatches(byte[] sealContext, byte[] relinKeys, List<byte[]> plaintextPolys,
                                           List<byte[]> ciphertextPolys, int psLowDegree);

    /**
     * 一般方法计算密文匹配结果。
     *
     * @param sealContext     SEAL上下文参数。
     * @param plaintextPolys  明文多项式。
     * @param ciphertextPolys 密文查询信息。
     * @return 密文匹配结果。
     */
    static native byte[] naiveComputeMatches(byte[] sealContext, List<byte[]> plaintextPolys, List<byte[]> ciphertextPolys);

    /**
     * 生成索引信息。
     *
     * @param sealContext SEAL上下文参数。
     * @param publicKey   公钥。
     * @param secretKey   私钥。
     * @param plainQuery  明文索引信息。
     * @return 索引信息。
     */
    static native List<byte[]> generateQuery(byte[] sealContext, byte[] publicKey, byte[] secretKey, long[][] plainQuery);

    /**
     * 解码查询结果。
     *
     * @param sealContext       SEAL上下文参数。
     * @param secretKey         私钥。
     * @param encryptedResponse 密文查询结果。
     * @return 查询结果。
     */
    static native long[] decodeReply(byte[] sealContext, byte[] secretKey, byte[] encryptedResponse);
}
