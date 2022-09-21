package edu.alibaba.mpc4j.common.tool.crypto.kyber.utils;

import edu.alibaba.mpc4j.common.tool.crypto.kyber.params.KyberParams;

import java.util.Arrays;

/**
 * Utility class for byte operations. Modified from:
 * <p>
 * https://github.com/fisherstevenk/kyberJCE/blob/main/src/main/java/com/swiftcryptollc/crypto/provider/kyber/ByteOps.java
 * </p>
 * The modification is for removing unnecessary import packages.
 *
 * @author Steven K Fisher, Sheng Hu.
 */
public final class ByteOps {

    /**
     * Returns a 32-bit unsigned integer as a long from byte x (only involves the first 4 bytes, little-endian).
     *
     * @param x byte array.
     * @return convert result.
     */
    public static long convertByteTo32BitUnsignedInt(byte[] x) {
        long r = (x[0] & 0xFF);
        r = r | ((long) (x[1] & 0xFF) << 8);
        r = r | ((long) (x[2] & 0xFF) << 16);
        r = r | ((long) (x[3] & 0xFF) << 24);
        return r;
    }

    /**
     * Returns a 24-bit unsigned integer as a long from byte x (only involves the first 4 bytes, little-endian).
     *
     * @param x byte array.
     * @return convert result.
     */
    public static long convertByteTo24BitUnsignedInt(byte[] x) {
        long r = (x[0] & 0xFF);
        r = r | ((long) (x[1] & 0xFF) << 8);
        r = r | ((long) (x[2] & 0xFF) << 16);
        return r;
    }

    /**
     * Generate a polynomial with coefficients distributed according to a centered binomial distribution with parameter
     * η, given an array of uniformly random bytes.
     *
     * @param prfOutput Prf output.
     * @param paramsK   parameter K, can be 2, 3 or 4.
     * @return A polynomial with coefficients distributed according to a centered binomial distribution.
     */
    public static short[] generateCbdPoly(byte[] prfOutput, int paramsK) {
        long t, d;
        int a, b;
        short[] r = new short[KyberParams.PARAMS_N];
        switch (paramsK) {
            case 2:
                for (int i = 0; i < KyberParams.PARAMS_N / KyberParams.MATH_FOUR; i++) {
                    t = ByteOps.convertByteTo24BitUnsignedInt(Arrays.copyOfRange(prfOutput, (3 * i), prfOutput.length));
                    d = t & 0x00249249;
                    d = d + ((t >> 1) & 0x00249249);
                    d = d + ((t >> 2) & 0x00249249);
                    for (int j = 0; j < KyberParams.MATH_FOUR; j++) {
                        a = (short) ((d >> (6 * j)) & 0x7);
                        b = (short) ((d >> (6 * j + KyberParams.ETA_512)) & 0x7);
                        r[4 * i + j] = (short) (a - b);
                    }
                }
                break;
            case 3:
            case 4:
                for (int i = 0; i < KyberParams.PARAMS_N / KyberParams.MATH_EIGHT; i++) {
                    t = ByteOps.convertByteTo32BitUnsignedInt(Arrays.copyOfRange(prfOutput, (4 * i), prfOutput.length));
                    d = t & 0x55555555;
                    d = d + ((t >> 1) & 0x55555555);
                    for (int j = 0; j < KyberParams.MATH_EIGHT; j++) {
                        a = (short) ((d >> (4 * j)) & 0x3);
                        b = (short) ((d >> (4 * j + KyberParams.ETA_768_1024)) & 0x3);
                        r[8 * i + j] = (short) (a - b);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException(KyberParams.INVALID_PARAMS_K_ERROR_MESSAGE + paramsK);
        }
        return r;
    }

    /**
     * Computes a Montgomery reduction given a 32-Bit Integer.
     *
     * @param a 32-Bit Integer.
     * @return reduced integer mod q.
     */
    public static short montgomeryReduce(long a) {
        short u = (short) (a * KyberParams.PARAMS_Q_INV);
        int t = (u * KyberParams.PARAMS_Q);
        t = (int) (a - t);
        t >>= 16;
        return (short) t;
    }

    /**
     * Computes a Barrett reduction given a 16-Bit Integer.
     *
     * @param a 16-Bit Integer.
     * @return reduced integer mod q.
     */
    public static short barrettReduce(short a) {
        short t;
        long shift = (((long) 1) << 26);
        short v = (short) ((shift + (KyberParams.PARAMS_Q / 2)) / KyberParams.PARAMS_Q);
        t = (short) ((v * a) >> 26);
        t = (short) (t * KyberParams.PARAMS_Q);
        return (short) (a - t);
    }

    /**
     * Conditionally subtract Q (from KyberParams) from the input a (i.e., compute a mod Q).
     * <p>
     * If the input is greater than or equal to Q = 3329, then subtract a by Q;
     * </p>
     * <p>
     * Else, remains the input as the same (even if the input is a negative number).
     * </p>
     *
     * @param a 16-Bit Integer.
     * @return a mod q.
     */
    public static short conditionalSubQ(short a) {
        a = (short) (a - KyberParams.PARAMS_Q);
        a = (short) (a + (((int) a >> 15) & KyberParams.PARAMS_Q));
        return a;
    }
}
