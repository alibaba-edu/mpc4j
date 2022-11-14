package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19;

import java.util.ArrayList;

/**
 * RSS19-核Zp64三元组生成协议本地工具类。
 *
 * @author Weiran Liu
 * @date 2022/11/5
 */
public class Rss19Zp64CoreMtgNativeUtils {

    private Rss19Zp64CoreMtgNativeUtils() {
        // empty
    }

    /**
     * 检查SEAL参数是否有效，返回明文模数。
     *
     * @param polyModulusDegree 模多项式阶。
     * @param plainModulusSize  明文模数比特长度。
     * @return 明文模数。
     */
    static native long checkCreatePlainModulus(int polyModulusDegree, int plainModulusSize);

    /**
     * 密钥生成。
     *
     * @param polyModulusDegree 模多项式阶。
     * @param plainModulus      明文模数。
     * @return 加密方案参数和密钥。
     */
    static native ArrayList<byte[]> keyGen(int polyModulusDegree, long plainModulus);

    /**
     * 加密函数。
     *
     * @param encryptionParams 加密方案参数。
     * @param publicKey        公钥。
     * @param secretKey        私钥。
     * @param plain1           明文1。
     * @param plain2           明文2。
     * @return 密文。
     */
    static native ArrayList<byte[]> encryption(byte[] encryptionParams, byte[] publicKey, byte[] secretKey, long[] plain1,
                                               long[] plain2);

    /**
     * 解密函数。
     *
     * @param encryptionParams 加密方案参数。
     * @param secretKey        私钥。
     * @param ciphertext       密文。
     * @return 明文。
     */
    static native long[] decryption(byte[] encryptionParams, byte[] secretKey, byte[] ciphertext);

    /**
     * 接收方密文计算函数。
     *
     * @param encryptionParams 加密方案参数。
     * @param cipher1          密文1。
     * @param cipher2          密文2。
     * @param plain1           明文1。
     * @param plain2           明文2。
     * @param randomMask       随机掩码。
     * @return 密文计算结果。
     */
    static native byte[] computeResponse(byte[] encryptionParams, byte[] cipher1, byte[] cipher2, long[] plain1,
                                         long[] plain2, long[] randomMask);
}
