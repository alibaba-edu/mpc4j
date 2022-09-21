package edu.alibaba.mpc4j.common.tool.crypto.kyber.params;

/**
 * Helper class for various static byte sizes. Modified from:
 * <p>
 * https://github.com/fisherstevenk/kyberJCE/blob/main/src/main/java/com/swiftcryptollc/crypto/provider/kyber/KyberParams.java
 * </p>
 *
 * @author Steven K Fisher, Sheng Hu, Weiran Liu.
 */
public final class KyberParams {
    /**
     * 整数值2
     */
    public static final int MATH_TWO = 2;
    /**
     * 整数值3
     */
    public static final int MATH_THREE = 3;
    /**
     * 整数值4
     */
    public static final int MATH_FOUR = 4;
    /**
     * 整数值8
     */
    public static final int MATH_EIGHT = 8;
    /**
     * 多项式系数数量，即环Zq[X] / (X^n + 1)中n的值
     */
    public static final int PARAMS_N = 256;
    /**
     * NTT的查找表大小
     */
    public static final int PARAMS_NTT_NUM = 128;
    /**
     * 有限域Zq的模数q
     */
    public static final int PARAMS_Q = 3329;
    /**
     * q^{-1} mod 2^16，即62209 = 3329^{-1} mod 65536
     */
    public static final int PARAMS_Q_INV = 62209;
    /**
     * 多项式字节长度，log_2(q) = 12，多项式共有256个系数，共3072比特，即384字节
     */
    public static final int POLY_BYTES = 384;
    /**
     * K = 2时，加密时所需噪声的多项式数量
     */
    public static final int ETA_512 = 3;
    /**
     * K = 3或K = 4时，加密时所需噪声的多项式数量
     */
    public static final int ETA_768_1024 = 2;
    /**
     * 噪声/随机数种子/明文消息字节长度，均为256比特，即32字节
     */
    public static final int SYM_BYTES = 32;
    /**
     * K = 2或K = 3时，压缩后的多项式字节长度
     */
    public static final int POLY_COMPRESSED_BYTES_768 = 128;
    /**
     * K = 4时，压缩后的多项式字节长度
     */
    public static final int POLY_COMPRESSED_BYTES_1024 = 160;
    /**
     * K = 2时，压缩后的多项式向量字节长度
     */
    public static final int POLY_VECTOR_COMPRESSED_BYTES_512 = 2 * 320;
    /**
     * K = 3时，压缩后的多项式向量字节长度
     */
    public static final int POLY_VECTOR_COMPRESSED_BYTES_768 = 3 * 320;
    /**
     * K = 4时，压缩后的多项式向量字节长度
     */
    public static final int POLY_VECTOR_COMPRESSED_BYTES_1024 = 4 * 352;
    /**
     * K = 2时的公钥长度
     */
    public static final int POLY_VECTOR_BYTES_512 = 2 * POLY_BYTES;
    /**
     * K = 3时的公钥长度
     */
    public static final int POLY_VECTOR_BYTES_768 = 3 * POLY_BYTES;
    /**
     * K = 4时的公钥长度
     */
    public static final int POLY_VECTOR_BYTES_1024 = 4 * POLY_BYTES;
    /**
     * 非法参数K的错误消息
     */
    public static final String INVALID_PARAMS_K_ERROR_MESSAGE = "Invalid K, must be 2, 3 or 4: ";

    /**
     * 验证K是否有效。
     *
     * @param paramsK 参数K。
     * @return 如果参数K有效，则返回{@code true}，否则返回{@code false}。
     */
    public static boolean validParamsK(int paramsK) {
        return paramsK == 2 || paramsK == 3 || paramsK == 4;
    }
}
