package edu.alibaba.mpc4j.common.tool.crypto.kyber.utils;

import edu.alibaba.mpc4j.common.tool.crypto.kyber.params.KyberParams;

/**
 * Number Theoretic Transform (NTT) Helper class. Modified from:
 * <p>
 * https://github.com/fisherstevenk/kyberJCE/blob/main/src/main/java/com/swiftcryptollc/crypto/provider/kyber/Ntt.java
 * </p>
 * The modification is for removing unnecessary import packages.
 *
 * @author Steven K Fisher, Sheng Hu.
 */
final class Ntt {
    /**
     * precomputed tables of powers of ζ^+[0], ..., ζ^+[127] in the bit-reverse order (with 7 bits) for ζ = 17,
     * MONT = 2285, and ζ^+[i] = ζ^i * MONT. The bit-reverse order map i -> brv_7[i] is:
     * <p> 00, 64, 32, 096, 16, 80, 48, 112, 08, 72, 40, 104, 24, 88, 56, 120,</p>
     * <p> 04, 68, 36, 100, 20, 84, 52, 116, 12, 76, 44, 108, 28, 92, 60, 124,</p>
     * <p> 02, 66, 34, 098, 18, 82, 50, 114, 10, 74, 42, 106, 26, 90, 58, 122,</p>
     * <p> 06, 70, 38, 102, 22, 86, 54, 118, 14, 78, 46, 110, 30, 94, 62, 126,</p>
     * <p> 01, 65, 33, 097, 17, 81, 49, 113, 09, 73, 41, 105, 25, 89, 57, 121,</p>
     * <p> 05, 69, 37, 101, 21, 85, 53, 117, 13, 77, 45, 109, 29, 93, 61, 125,</p>
     * <p> 03, 67, 35, 099, 19, 83, 51, 115, 11, 75, 43, 107, 27, 91, 59, 123,</p>
     * <p> 07, 71, 39, 103, 23, 87, 55, 119, 15, 79, 47, 111, 31, 95, 63, 127,</p>
     * We compute temp[i] = ζ^i * MONT, and then set ζ^+[i] = temp[map[i]].
     * <p>For detailed explanation, see https://zhuanlan.zhihu.com/p/577710663.</p>
     */
    static final short[] NTT_ZETAS = new short[]{
        2285, 2571, 2970, 1812, 1493, 1422, 287, 202, 3158, 622, 1577, 182, 962, 2127, 1855, 1468,
        573, 2004, 264, 383, 2500, 1458, 1727, 3199, 2648, 1017, 732, 608, 1787, 411, 3124, 1758,
        1223, 652, 2777, 1015, 2036, 1491, 3047, 1785, 516, 3321, 3009, 2663, 1711, 2167, 126, 1469,
        2476, 3239, 3058, 830, 107, 1908, 3082, 2378, 2931, 961, 1821, 2604, 448, 2264, 677, 2054,
        2226, 430, 555, 843, 2078, 871, 1550, 105, 422, 587, 177, 3094, 3038, 2869, 1574, 1653,
        3083, 778, 1159, 3182, 2552, 1483, 2727, 1119, 1739, 644, 2457, 349, 418, 329, 3173, 3254,
        817, 1097, 603, 610, 1322, 2044, 1864, 384, 2114, 3193, 1218, 1994, 2455, 220, 2142, 1670,
        2144, 1799, 2051, 794, 1819, 2475, 2459, 478, 3221, 3021, 996, 991, 958, 1869, 1522, 1628,
    };
    /**
     * precomputed tables of powers of ζ^-[0], ..., ζ^-[127] in the bit-reverse order (with 7 bits) for ζ = 17,
     * MONT = 2285, ζ^-[i - 1] = ζ^{-i} * MONT, finally ζ^-[127]. The bit-reverse order map i -> brv_7[i] is:
     * <p> 00, 64, 32, 096, 16, 80, 48, 112, 08, 72, 40, 104, 24, 88, 56, 120,</p>
     * <p> 04, 68, 36, 100, 20, 84, 52, 116, 12, 76, 44, 108, 28, 92, 60, 124,</p>
     * <p> 02, 66, 34, 098, 18, 82, 50, 114, 10, 74, 42, 106, 26, 90, 58, 122,</p>
     * <p> 06, 70, 38, 102, 22, 86, 54, 118, 14, 78, 46, 110, 30, 94, 62, 126,</p>
     * <p> 01, 65, 33, 097, 17, 81, 49, 113, 09, 73, 41, 105, 25, 89, 57, 121,</p>
     * <p> 05, 69, 37, 101, 21, 85, 53, 117, 13, 77, 45, 109, 29, 93, 61, 125,</p>
     * <p> 03, 67, 35, 099, 19, 83, 51, 115, 11, 75, 43, 107, 27, 91, 59, 123,</p>
     * <p> 07, 71, 39, 103, 23, 87, 55, 119, 15, 79, 47, 111, 31, 95, 63, 127,</p>
     * We compute temp[i - 1] = ζ^{-i} * MONT, temp[127], and then set ζ^-[i] = temp[map[i]].
     * <p>For detailed explanation, see https://zhuanlan.zhihu.com/p/577710663.</p>
     */
    static final short[] NTT_ZETAS_INV = new short[]{
        1701, 1807, 1460, 2371, 2338, 2333, 308, 108, 2851, 870, 854, 1510, 2535, 1278, 1530, 1185,
        1659, 1187, 3109, 874, 1335, 2111, 136, 1215, 2945, 1465, 1285, 2007, 2719, 2726, 2232, 2512,
        75, 156, 3000, 2911, 2980, 872, 2685, 1590, 2210, 602, 1846, 777, 147, 2170, 2551, 246,
        1676, 1755, 460, 291, 235, 3152, 2742, 2907, 3224, 1779, 2458, 1251, 2486, 2774, 2899, 1103,
        1275, 2652, 1065, 2881, 725, 1508, 2368, 398, 951, 247, 1421, 3222, 2499, 271, 90, 853,
        1860, 3203, 1162, 1618, 666, 320, 8, 2813, 1544, 282, 1838, 1293, 2314, 552, 2677, 2106,
        1571, 205, 2918, 1542, 2721, 2597, 2312, 681, 130, 1602, 1871, 829, 2946, 3065, 1325, 2756,
        1861, 1474, 1202, 2367, 3147, 1752, 2707, 171, 3127, 3042, 1907, 1836, 1517, 359, 758, 1441
    };

    /**
     * Multiply the given shorts and then run a Montgomery reduce.
     *
     * @param a input a。
     * @param b input b。
     * @return 乘法结果。
     */
    static short modqMulMontgomery(short a, short b) {
        return ByteOps.montgomeryReduce((long) a * (long) b);
    }

    /**
     * Perform an in-place number-theoretic transform (NTT).
     * Input is in standard order. Output is in bit-reversed order.
     *
     * @param poly polynomial.
     */
    static void inNtt(short[] poly) {
        int j;
        int k = 1;
        // 使用蝴蝶（Butterfly）算法实现的快速傅里叶变换（Fast DFT）
        for (int l = KyberParams.PARAMS_NTT_NUM; l >= KyberParams.MATH_TWO; l >>= 1) {
            for (int start = 0; start < KyberParams.PARAMS_N; start = j + l) {
                short zeta = Ntt.NTT_ZETAS[k];
                k = k + 1;
                for (j = start; j < start + l; j++) {
                    short t = Ntt.modqMulMontgomery(zeta, poly[j + l]);
                    poly[j + l] = (short) (poly[j] - t);
                    poly[j] = (short) (poly[j] + t);
                }
            }
        }
    }

    /**
     * Perform an in-place inverse number-theoretic transform (NTT).
     * Input is in bit-reversed order. Output is in standard order.
     *
     * @param nttPolynomial NTT polynomial.
     */
    static void inInvNtt(short[] nttPolynomial) {
        int j;
        int k = 0;
        // // 使用蝴蝶（Butterfly）算法实现的快速傅里叶拟变换（Fast inverse DFT）
        for (int l = 2; l <= KyberParams.PARAMS_NTT_NUM; l <<= 1) {
            for (int start = 0; start < KyberParams.PARAMS_N; start = j + l) {
                short zeta = Ntt.NTT_ZETAS_INV[k];
                k = k + 1;
                for (j = start; j < start + l; j++) {
                    short t = nttPolynomial[j];
                    nttPolynomial[j] = ByteOps.barrettReduce((short) (t + nttPolynomial[j + l]));
                    nttPolynomial[j + l] = (short) (t - nttPolynomial[j + l]);
                    nttPolynomial[j + l] = modqMulMontgomery(zeta, nttPolynomial[j + l]);
                }
            }
        }
        for (j = 0; j < KyberParams.PARAMS_N; j++) {
            nttPolynomial[j] = Ntt.modqMulMontgomery(nttPolynomial[j], NTT_ZETAS_INV[127]);
        }
    }

    /**
     * Performs the multiplication of a polynomial coefficient with parameter ξ.
     *
     * @param a0   a[h].
     * @param a1   a[l].
     * @param b0   b[h].
     * @param b1   b[l].
     * @param zeta ξ.
     * @return multiplication result r[h], r[l].
     */
    static short[] baseMultiplier(short a0, short a1, short b0, short b1, short zeta) {
        short[] r = new short[2];
        r[0] = Ntt.modqMulMontgomery(a1, b1);
        r[0] = Ntt.modqMulMontgomery(r[0], zeta);
        r[0] = (short) (r[0] + Ntt.modqMulMontgomery(a0, b0));
        r[1] = Ntt.modqMulMontgomery(a0, b1);
        r[1] = (short) (r[1] + Ntt.modqMulMontgomery(a1, b0));
        return r;
    }
}
