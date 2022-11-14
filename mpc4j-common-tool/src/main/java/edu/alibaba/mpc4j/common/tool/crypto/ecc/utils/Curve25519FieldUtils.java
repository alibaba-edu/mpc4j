package edu.alibaba.mpc4j.common.tool.crypto.ecc.utils;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe.CafeConstantTimeUtils;

/**
 * Curve25519有限域工具类。
 *
 * @author Weiran Liu
 * @date 2022/11/13
 */
public class Curve25519FieldUtils {

    private Curve25519FieldUtils() {
        // empty
    }

    /**
     * field int size
     */
    public static final int INT_SIZE = 10;
    /**
     * field byte size
     */
    public static final int BYTE_SIZE = 32;
    /**
     * 0
     */
    public static final int[] ZERO_INTS = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0,};
    /**
     * 0 in byte array
     */
    private static final byte[] ZERO_BYTES = new byte[]{
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };
    /**
     * 1
     */
    public static final int[] ONE_INTS = new int[]{1, 0, 0, 0, 0, 0, 0, 0, 0, 0,};
    /**
     * -1
     */
    public static final int[] MINUS_ONE_INTS = new int[]{-1, 0, 0, 0, 0, 0, 0, 0, 0, 0,};
    /**
     * a
     */
    public static final int[] A_INTS = new int[]{486662, 0, 0, 0, 0, 0, 0, 0, 0, 0,};
    /**
     * √(-(a + 2))
     */
    public static final int[] SQRT_MINUS_A_PLUS_2_INTS = new int[]{
        -12222970, -8312128, -11511410, 9067497, -15300785, -241793, 25456130, 14121551, -12187136, 3972024,
    };
    /**
     * √(-1 / 2)
     */
    public static final int[] SQRT_MINUS_HALF_INTS = new int[]{
        -17256545, 3971863, 28865457, -1750208, 27359696, -16640980, 12573105, 1002827, -163343, 11073975,
    };
    /**
     * Precomputed value of one of the square roots of -1 (mod p).
     */
    public static final int[] SQRT_M1_INTS = new int[]{
        -32595792, -7943725, 9377950, 3500415, 12389472, -272473, -25146209, -2005654, 326686, 11406482,
    };
    /**
     * Edwards $d$ value, equal to $-121665/121666 \bmod p$.
     */
    public static final int[] EDWARDS_D_INTS = new int[]{
        -10913610, 13857413, -15372611, 6949391, 114729, -8787816, -6275908, -3247719, -18696448, -12055116,
    };

    /**
     * Create a field element with value 0.
     *
     * @return a field element with value 0.
     */
    public static int[] createZero() {
        return new int[INT_SIZE];
    }

    /**
     * Create a field element with value 1.
     *
     * @return a field element with value 1.
     */
    public static int[] createOne() {
        int[] z = new int[INT_SIZE];
        z[0] = 1;
        return z;
    }

    /**
     * Encode the field element x in its 32-byte representation.
     *
     * @param x the field element x.
     * @return the 32-byte representation.
     */
    @SuppressWarnings("AlibabaMethodTooLong")
    public static byte[] encode(final int[] x) {
        int h0 = x[0];
        int h1 = x[1];
        int h2 = x[2];
        int h3 = x[3];
        int h4 = x[4];
        int h5 = x[5];
        int h6 = x[6];
        int h7 = x[7];
        int h8 = x[8];
        int h9 = x[9];
        int q;
        int carry0;
        int carry1;
        int carry2;
        int carry3;
        int carry4;
        int carry5;
        int carry6;
        int carry7;
        int carry8;
        int carry9;

        // Step 1:
        // Calculate q
        q = (19 * h9 + (1 << 24)) >> 25;
        q = (h0 + q) >> 26;
        q = (h1 + q) >> 25;
        q = (h2 + q) >> 26;
        q = (h3 + q) >> 25;
        q = (h4 + q) >> 26;
        q = (h5 + q) >> 25;
        q = (h6 + q) >> 26;
        q = (h7 + q) >> 25;
        q = (h8 + q) >> 26;
        q = (h9 + q) >> 25;

        // r = h - q * p = h - 2^255 * q + 19 * q
        // First add 19 * q then discard the bit 255
        h0 += 19 * q;

        carry0 = h0 >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;
        carry1 = h1 >> 25;
        h2 += carry1;
        h1 -= carry1 << 25;
        carry2 = h2 >> 26;
        h3 += carry2;
        h2 -= carry2 << 26;
        carry3 = h3 >> 25;
        h4 += carry3;
        h3 -= carry3 << 25;
        carry4 = h4 >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;
        carry5 = h5 >> 25;
        h6 += carry5;
        h5 -= carry5 << 25;
        carry6 = h6 >> 26;
        h7 += carry6;
        h6 -= carry6 << 26;
        carry7 = h7 >> 25;
        h8 += carry7;
        h7 -= carry7 << 25;
        carry8 = h8 >> 26;
        h9 += carry8;
        h8 -= carry8 << 26;
        carry9 = h9 >> 25;
        h9 -= carry9 << 25;

        // Step 2 (straight forward conversion):
        byte[] s = new byte[BYTE_SIZE];
        s[0] = (byte) h0;
        s[1] = (byte) (h0 >> 8);
        s[2] = (byte) (h0 >> 16);
        s[3] = (byte) ((h0 >> 24) | (h1 << 2));
        s[4] = (byte) (h1 >> 6);
        s[5] = (byte) (h1 >> 14);
        s[6] = (byte) ((h1 >> 22) | (h2 << 3));
        s[7] = (byte) (h2 >> 5);
        s[8] = (byte) (h2 >> 13);
        s[9] = (byte) ((h2 >> 21) | (h3 << 5));
        s[10] = (byte) (h3 >> 3);
        s[11] = (byte) (h3 >> 11);
        s[12] = (byte) ((h3 >> 19) | (h4 << 6));
        s[13] = (byte) (h4 >> 2);
        s[14] = (byte) (h4 >> 10);
        s[15] = (byte) (h4 >> 18);
        s[16] = (byte) h5;
        s[17] = (byte) (h5 >> 8);
        s[18] = (byte) (h5 >> 16);
        s[19] = (byte) ((h5 >> 24) | (h6 << 1));
        s[20] = (byte) (h6 >> 7);
        s[21] = (byte) (h6 >> 15);
        s[22] = (byte) ((h6 >> 23) | (h7 << 3));
        s[23] = (byte) (h7 >> 5);
        s[24] = (byte) (h7 >> 13);
        s[25] = (byte) ((h7 >> 21) | (h8 << 4));
        s[26] = (byte) (h8 >> 4);
        s[27] = (byte) (h8 >> 12);
        s[28] = (byte) ((h8 >> 20) | (h9 << 6));
        s[29] = (byte) (h9 >> 2);
        s[30] = (byte) (h9 >> 10);
        s[31] = (byte) (h9 >> 18);
        return s;
    }


    /**
     * Copy x to z.
     *
     * @param x the input x.
     * @param z the copied output z = x.
     */
    public static void copy(final int[] x, int[] z) {
        System.arraycopy(x, 0, z, 0, INT_SIZE);
    }

    /**
     * Compares the two field elements x and y.
     *
     * @return 1 if x == y, 0 otherwise.
     */
    public static int areEqual(final int[] x, final int[] y) {
        return CafeConstantTimeUtils.equal(encode(x), encode(y));
    }

    /**
     * Determine whether the field element x is zero.
     *
     * @return 1 if x is zero, 0 otherwise.
     */
    public static int isZero(final int[] x) {
        final byte[] s = encode(x);
        return CafeConstantTimeUtils.equal(s, ZERO_BYTES);
    }

    /**
     * Determine whether the field element x is negative.
     *
     * @return 1 if x is negative, 0 otherwise.
     */
    public static int isNeg(final int[] x) {
        final byte[] s = encode(x);
        return s[0] & 1;
    }

    /**
     * If cond = 1, then z = z; Otherwise, z = x.
     *
     * @param cond the condition.
     * @param x    the other input x.
     * @param z    the input z.
     */
    public static void cmov(final int cond, final int[] x, int[] z) {
        int c = -cond;
        for (int i = 0; i < INT_SIZE; i++) {
            int zi = z[i];
            int diff = zi ^ x[i];
            zi ^= (diff & c);
            z[i] = zi;
        }
    }

    /**
     * Compute z = x + y.
     *
     * @param x the input x.
     * @param y the input y.
     * @param z the output z = x + y.
     */
    public static void add(int[] x, final int[] y, int[] z) {
        for (int i = 0; i < INT_SIZE; i++) {
            z[i] = x[i] + y[i];
        }
    }

    /**
     * Compute z = x - y.
     *
     * @param x the input x.
     * @param y the input y.
     * @param z the output z = x - y.
     */
    public static void sub(int[] x, final int[] y, int[] z) {
        for (int i = 0; i < INT_SIZE; i++) {
            z[i] = x[i] - y[i];
        }
    }

    /**
     * Compute z = -x.
     *
     * @param x the input x.
     * @param z the output z = -x.
     */
    public static void neg(int[] x, int[] z) {
        for (int i = 0; i < INT_SIZE; i++) {
            z[i] = -x[i];
        }
    }

    /**
     * Compute z = x * y.
     *
     * @param x the input x.
     * @param y the input y.
     * @param z the output z = x * y.
     */
    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    public static void mul(int[] x, int[] y, int[] z) {
        // 1.959375*2^29
        int g1_19 = 19 * y[1];
        // 1.959375*2^30; still ok
        int g2_19 = 19 * y[2];
        int g3_19 = 19 * y[3];
        int g4_19 = 19 * y[4];
        int g5_19 = 19 * y[5];
        int g6_19 = 19 * y[6];
        int g7_19 = 19 * y[7];
        int g8_19 = 19 * y[8];
        int g9_19 = 19 * y[9];
        int f1_2 = 2 * x[1];
        int f3_2 = 2 * x[3];
        int f5_2 = 2 * x[5];
        int f7_2 = 2 * x[7];
        int f9_2 = 2 * x[9];

        long f0g0 = x[0] * (long) y[0];
        long f0g1 = x[0] * (long) y[1];
        long f0g2 = x[0] * (long) y[2];
        long f0g3 = x[0] * (long) y[3];
        long f0g4 = x[0] * (long) y[4];
        long f0g5 = x[0] * (long) y[5];
        long f0g6 = x[0] * (long) y[6];
        long f0g7 = x[0] * (long) y[7];
        long f0g8 = x[0] * (long) y[8];
        long f0g9 = x[0] * (long) y[9];
        long f1g0 = x[1] * (long) y[0];
        long f1g1_2 = f1_2 * (long) y[1];
        long f1g2 = x[1] * (long) y[2];
        long f1g3_2 = f1_2 * (long) y[3];
        long f1g4 = x[1] * (long) y[4];
        long f1g5_2 = f1_2 * (long) y[5];
        long f1g6 = x[1] * (long) y[6];
        long f1g7_2 = f1_2 * (long) y[7];
        long f1g8 = x[1] * (long) y[8];
        long f1g9_38 = f1_2 * (long) g9_19;
        long f2g0 = x[2] * (long) y[0];
        long f2g1 = x[2] * (long) y[1];
        long f2g2 = x[2] * (long) y[2];
        long f2g3 = x[2] * (long) y[3];
        long f2g4 = x[2] * (long) y[4];
        long f2g5 = x[2] * (long) y[5];
        long f2g6 = x[2] * (long) y[6];
        long f2g7 = x[2] * (long) y[7];
        long f2g8_19 = x[2] * (long) g8_19;
        long f2g9_19 = x[2] * (long) g9_19;
        long f3g0 = x[3] * (long) y[0];
        long f3g1_2 = f3_2 * (long) y[1];
        long f3g2 = x[3] * (long) y[2];
        long f3g3_2 = f3_2 * (long) y[3];
        long f3g4 = x[3] * (long) y[4];
        long f3g5_2 = f3_2 * (long) y[5];
        long f3g6 = x[3] * (long) y[6];
        long f3g7_38 = f3_2 * (long) g7_19;
        long f3g8_19 = x[3] * (long) g8_19;
        long f3g9_38 = f3_2 * (long) g9_19;
        long f4g0 = x[4] * (long) y[0];
        long f4g1 = x[4] * (long) y[1];
        long f4g2 = x[4] * (long) y[2];
        long f4g3 = x[4] * (long) y[3];
        long f4g4 = x[4] * (long) y[4];
        long f4g5 = x[4] * (long) y[5];
        long f4g6_19 = x[4] * (long) g6_19;
        long f4g7_19 = x[4] * (long) g7_19;
        long f4g8_19 = x[4] * (long) g8_19;
        long f4g9_19 = x[4] * (long) g9_19;
        long f5g0 = x[5] * (long) y[0];
        long f5g1_2 = f5_2 * (long) y[1];
        long f5g2 = x[5] * (long) y[2];
        long f5g3_2 = f5_2 * (long) y[3];
        long f5g4 = x[5] * (long) y[4];
        long f5g5_38 = f5_2 * (long) g5_19;
        long f5g6_19 = x[5] * (long) g6_19;
        long f5g7_38 = f5_2 * (long) g7_19;
        long f5g8_19 = x[5] * (long) g8_19;
        long f5g9_38 = f5_2 * (long) g9_19;
        long f6g0 = x[6] * (long) y[0];
        long f6g1 = x[6] * (long) y[1];
        long f6g2 = x[6] * (long) y[2];
        long f6g3 = x[6] * (long) y[3];
        long f6g4_19 = x[6] * (long) g4_19;
        long f6g5_19 = x[6] * (long) g5_19;
        long f6g6_19 = x[6] * (long) g6_19;
        long f6g7_19 = x[6] * (long) g7_19;
        long f6g8_19 = x[6] * (long) g8_19;
        long f6g9_19 = x[6] * (long) g9_19;
        long f7g0 = x[7] * (long) y[0];
        long f7g1_2 = f7_2 * (long) y[1];
        long f7g2 = x[7] * (long) y[2];
        long f7g3_38 = f7_2 * (long) g3_19;
        long f7g4_19 = x[7] * (long) g4_19;
        long f7g5_38 = f7_2 * (long) g5_19;
        long f7g6_19 = x[7] * (long) g6_19;
        long f7g7_38 = f7_2 * (long) g7_19;
        long f7g8_19 = x[7] * (long) g8_19;
        long f7g9_38 = f7_2 * (long) g9_19;
        long f8g0 = x[8] * (long) y[0];
        long f8g1 = x[8] * (long) y[1];
        long f8g2_19 = x[8] * (long) g2_19;
        long f8g3_19 = x[8] * (long) g3_19;
        long f8g4_19 = x[8] * (long) g4_19;
        long f8g5_19 = x[8] * (long) g5_19;
        long f8g6_19 = x[8] * (long) g6_19;
        long f8g7_19 = x[8] * (long) g7_19;
        long f8g8_19 = x[8] * (long) g8_19;
        long f8g9_19 = x[8] * (long) g9_19;
        long f9g0 = x[9] * (long) y[0];
        long f9g1_38 = f9_2 * (long) g1_19;
        long f9g2_19 = x[9] * (long) g2_19;
        long f9g3_38 = f9_2 * (long) g3_19;
        long f9g4_19 = x[9] * (long) g4_19;
        long f9g5_38 = f9_2 * (long) g5_19;
        long f9g6_19 = x[9] * (long) g6_19;
        long f9g7_38 = f9_2 * (long) g7_19;
        long f9g8_19 = x[9] * (long) g8_19;
        long f9g9_38 = f9_2 * (long) g9_19;

        /*
         * Remember: 2^255 congruent 19 modulo p. h = h0 * 2^0 + h1 * 2^26 + h2 *
         * 2^(26+25) + h3 * 2^(26+25+26) + ... + h9 * 2^(5*26+5*25). So to get the real
         * number we would have to multiply the coefficients with the corresponding
         * powers of 2. To get an idea what is going on below, look at the calculation
         * of h0: h0 is the coefficient to the power 2^0 so it collects (sums) all
         * products that have the power 2^0. f0 * g0 really is f0 * 2^0 * g0 * 2^0 = (f0
         * * g0) * 2^0. f1 * g9 really is f1 * 2^26 * g9 * 2^230 = f1 * g9 * 2^256 = 2 *
         * f1 * g9 * 2^255 congruent 2 * 19 * f1 * g9 * 2^0 modulo p. f2 * g8 really is
         * f2 * 2^51 * g8 * 2^204 = f2 * g8 * 2^255 congruent 19 * f2 * g8 * 2^0 modulo
         * p. and so on...
         */
        long h0 = f0g0 + f1g9_38 + f2g8_19 + f3g7_38 + f4g6_19 + f5g5_38 + f6g4_19 + f7g3_38 + f8g2_19 + f9g1_38;
        long h1 = f0g1 + f1g0 + f2g9_19 + f3g8_19 + f4g7_19 + f5g6_19 + f6g5_19 + f7g4_19 + f8g3_19 + f9g2_19;
        long h2 = f0g2 + f1g1_2 + f2g0 + f3g9_38 + f4g8_19 + f5g7_38 + f6g6_19 + f7g5_38 + f8g4_19 + f9g3_38;
        long h3 = f0g3 + f1g2 + f2g1 + f3g0 + f4g9_19 + f5g8_19 + f6g7_19 + f7g6_19 + f8g5_19 + f9g4_19;
        long h4 = f0g4 + f1g3_2 + f2g2 + f3g1_2 + f4g0 + f5g9_38 + f6g8_19 + f7g7_38 + f8g6_19 + f9g5_38;
        long h5 = f0g5 + f1g4 + f2g3 + f3g2 + f4g1 + f5g0 + f6g9_19 + f7g8_19 + f8g7_19 + f9g6_19;
        long h6 = f0g6 + f1g5_2 + f2g4 + f3g3_2 + f4g2 + f5g1_2 + f6g0 + f7g9_38 + f8g8_19 + f9g7_38;
        long h7 = f0g7 + f1g6 + f2g5 + f3g4 + f4g3 + f5g2 + f6g1 + f7g0 + f8g9_19 + f9g8_19;
        long h8 = f0g8 + f1g7_2 + f2g6 + f3g5_2 + f4g4 + f5g3_2 + f6g2 + f7g1_2 + f8g0 + f9g9_38;
        long h9 = f0g9 + f1g8 + f2g7 + f3g6 + f4g5 + f5g4 + f6g3 + f7g2 + f8g1 + f9g0;

        long carry0;
        long carry1;
        long carry2;
        long carry3;
        long carry4;
        long carry5;
        long carry6;
        long carry7;
        long carry8;
        long carry9;

        /*
         * |h0| <= (1.65*1.65*2^52*(1+19+19+19+19)+1.65*1.65*2^50*(38+38+38+38+38)) i.e.
         * |h0| <= 1.4*2^60; narrower ranges for h2, h4, h6, h8 |h1| <=
         * (1.65*1.65*2^51*(1+1+19+19+19+19+19+19+19+19)) i.e. |h1| <= 1.7*2^59;
         * narrower ranges for h3, h5, h7, h9
         */

        carry0 = (h0 + (long) (1 << 25)) >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;
        carry4 = (h4 + (long) (1 << 25)) >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;
        /* |h0| <= 2^25 */
        /* |h4| <= 2^25 */
        /* |h1| <= 1.71*2^59 */
        /* |h5| <= 1.71*2^59 */

        carry1 = (h1 + (long) (1 << 24)) >> 25;
        h2 += carry1;
        h1 -= carry1 << 25;
        carry5 = (h5 + (long) (1 << 24)) >> 25;
        h6 += carry5;
        h5 -= carry5 << 25;
        /* |h1| <= 2^24; from now on fits into int32 */
        /* |h5| <= 2^24; from now on fits into int32 */
        /* |h2| <= 1.41*2^60 */
        /* |h6| <= 1.41*2^60 */

        carry2 = (h2 + (long) (1 << 25)) >> 26;
        h3 += carry2;
        h2 -= carry2 << 26;
        carry6 = (h6 + (long) (1 << 25)) >> 26;
        h7 += carry6;
        h6 -= carry6 << 26;
        /* |h2| <= 2^25; from now on fits into int32 unchanged */
        /* |h6| <= 2^25; from now on fits into int32 unchanged */
        /* |h3| <= 1.71*2^59 */
        /* |h7| <= 1.71*2^59 */

        carry3 = (h3 + (long) (1 << 24)) >> 25;
        h4 += carry3;
        h3 -= carry3 << 25;
        carry7 = (h7 + (long) (1 << 24)) >> 25;
        h8 += carry7;
        h7 -= carry7 << 25;
        /* |h3| <= 2^24; from now on fits into int32 unchanged */
        /* |h7| <= 2^24; from now on fits into int32 unchanged */
        /* |h4| <= 1.72*2^34 */
        /* |h8| <= 1.41*2^60 */

        carry4 = (h4 + (long) (1 << 25)) >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;
        carry8 = (h8 + (long) (1 << 25)) >> 26;
        h9 += carry8;
        h8 -= carry8 << 26;
        /* |h4| <= 2^25; from now on fits into int32 unchanged */
        /* |h8| <= 2^25; from now on fits into int32 unchanged */
        /* |h5| <= 1.01*2^24 */
        /* |h9| <= 1.71*2^59 */

        carry9 = (h9 + (long) (1 << 24)) >> 25;
        h0 += carry9 * 19;
        h9 -= carry9 << 25;
        /* |h9| <= 2^24; from now on fits into int32 unchanged */
        /* |h0| <= 1.1*2^39 */

        carry0 = (h0 + (long) (1 << 25)) >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;
        /* |h0| <= 2^25; from now on fits into int32 unchanged */
        /* |h1| <= 1.01*2^24 */

        z[0] = (int) h0;
        z[1] = (int) h1;
        z[2] = (int) h2;
        z[3] = (int) h3;
        z[4] = (int) h4;
        z[5] = (int) h5;
        z[6] = (int) h6;
        z[7] = (int) h7;
        z[8] = (int) h8;
        z[9] = (int) h9;
    }

    /**
     * Compute z = x^2 = x * x.
     *
     * @param x the input x.
     * @param z the output z = x^2.
     */
    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    public static void sqr(int[] x, int[] z) {
        int f0 = x[0];
        int f1 = x[1];
        int f2 = x[2];
        int f3 = x[3];
        int f4 = x[4];
        int f5 = x[5];
        int f6 = x[6];
        int f7 = x[7];
        int f8 = x[8];
        int f9 = x[9];
        int f0_2 = 2 * f0;
        int f1_2 = 2 * f1;
        int f2_2 = 2 * f2;
        int f3_2 = 2 * f3;
        int f4_2 = 2 * f4;
        int f5_2 = 2 * f5;
        int f6_2 = 2 * f6;
        int f7_2 = 2 * f7;
        // 1.959375*2^30
        int f5_38 = 38 * f5;
        // 1.959375*2^30
        int f6_19 = 19 * f6;
        // 1.959375*2^30
        int f7_38 = 38 * f7;
        // 1.959375*2^30
        int f8_19 = 19 * f8;
        // 1.959375*2^30
        int f9_38 = 38 * f9;

        long f0f0 = f0 * (long) f0;
        long f0f1_2 = f0_2 * (long) f1;
        long f0f2_2 = f0_2 * (long) f2;
        long f0f3_2 = f0_2 * (long) f3;
        long f0f4_2 = f0_2 * (long) f4;
        long f0f5_2 = f0_2 * (long) f5;
        long f0f6_2 = f0_2 * (long) f6;
        long f0f7_2 = f0_2 * (long) f7;
        long f0f8_2 = f0_2 * (long) f8;
        long f0f9_2 = f0_2 * (long) f9;
        long f1f1_2 = f1_2 * (long) f1;
        long f1f2_2 = f1_2 * (long) f2;
        long f1f3_4 = f1_2 * (long) f3_2;
        long f1f4_2 = f1_2 * (long) f4;
        long f1f5_4 = f1_2 * (long) f5_2;
        long f1f6_2 = f1_2 * (long) f6;
        long f1f7_4 = f1_2 * (long) f7_2;
        long f1f8_2 = f1_2 * (long) f8;
        long f1f9_76 = f1_2 * (long) f9_38;
        long f2f2 = f2 * (long) f2;
        long f2f3_2 = f2_2 * (long) f3;
        long f2f4_2 = f2_2 * (long) f4;
        long f2f5_2 = f2_2 * (long) f5;
        long f2f6_2 = f2_2 * (long) f6;
        long f2f7_2 = f2_2 * (long) f7;
        long f2f8_38 = f2_2 * (long) f8_19;
        long f2f9_38 = f2 * (long) f9_38;
        long f3f3_2 = f3_2 * (long) f3;
        long f3f4_2 = f3_2 * (long) f4;
        long f3f5_4 = f3_2 * (long) f5_2;
        long f3f6_2 = f3_2 * (long) f6;
        long f3f7_76 = f3_2 * (long) f7_38;
        long f3f8_38 = f3_2 * (long) f8_19;
        long f3f9_76 = f3_2 * (long) f9_38;
        long f4f4 = f4 * (long) f4;
        long f4f5_2 = f4_2 * (long) f5;
        long f4f6_38 = f4_2 * (long) f6_19;
        long f4f7_38 = f4 * (long) f7_38;
        long f4f8_38 = f4_2 * (long) f8_19;
        long f4f9_38 = f4 * (long) f9_38;
        long f5f5_38 = f5 * (long) f5_38;
        long f5f6_38 = f5_2 * (long) f6_19;
        long f5f7_76 = f5_2 * (long) f7_38;
        long f5f8_38 = f5_2 * (long) f8_19;
        long f5f9_76 = f5_2 * (long) f9_38;
        long f6f6_19 = f6 * (long) f6_19;
        long f6f7_38 = f6 * (long) f7_38;
        long f6f8_38 = f6_2 * (long) f8_19;
        long f6f9_38 = f6 * (long) f9_38;
        long f7f7_38 = f7 * (long) f7_38;
        long f7f8_38 = f7_2 * (long) f8_19;
        long f7f9_76 = f7_2 * (long) f9_38;
        long f8f8_19 = f8 * (long) f8_19;
        long f8f9_38 = f8 * (long) f9_38;
        long f9f9_38 = f9 * (long) f9_38;

        /*
         * Same procedure as in multiply, but this time we have a higher symmetry leading to less summands. e.g.
         * f1f9_76 really stands for f1 * 2^26 * f9 * 2^230 + f9 * 2^230 + f1 * 2^26 congruent 2 * 2 * 19 * f1 * f9 2^0
         * modulo p.
         */
        long h0 = f0f0 + f1f9_76 + f2f8_38 + f3f7_76 + f4f6_38 + f5f5_38;
        long h1 = f0f1_2 + f2f9_38 + f3f8_38 + f4f7_38 + f5f6_38;
        long h2 = f0f2_2 + f1f1_2 + f3f9_76 + f4f8_38 + f5f7_76 + f6f6_19;
        long h3 = f0f3_2 + f1f2_2 + f4f9_38 + f5f8_38 + f6f7_38;
        long h4 = f0f4_2 + f1f3_4 + f2f2 + f5f9_76 + f6f8_38 + f7f7_38;
        long h5 = f0f5_2 + f1f4_2 + f2f3_2 + f6f9_38 + f7f8_38;
        long h6 = f0f6_2 + f1f5_4 + f2f4_2 + f3f3_2 + f7f9_76 + f8f8_19;
        long h7 = f0f7_2 + f1f6_2 + f2f5_2 + f3f4_2 + f8f9_38;
        long h8 = f0f8_2 + f1f7_4 + f2f6_2 + f3f5_4 + f4f4 + f9f9_38;
        long h9 = f0f9_2 + f1f8_2 + f2f7_2 + f3f6_2 + f4f5_2;

        long carry0;
        long carry1;
        long carry2;
        long carry3;
        long carry4;
        long carry5;
        long carry6;
        long carry7;
        long carry8;
        long carry9;

        carry0 = (h0 + (long) (1 << 25)) >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;
        carry4 = (h4 + (long) (1 << 25)) >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;

        carry1 = (h1 + (long) (1 << 24)) >> 25;
        h2 += carry1;
        h1 -= carry1 << 25;
        carry5 = (h5 + (long) (1 << 24)) >> 25;
        h6 += carry5;
        h5 -= carry5 << 25;

        carry2 = (h2 + (long) (1 << 25)) >> 26;
        h3 += carry2;
        h2 -= carry2 << 26;
        carry6 = (h6 + (long) (1 << 25)) >> 26;
        h7 += carry6;
        h6 -= carry6 << 26;

        carry3 = (h3 + (long) (1 << 24)) >> 25;
        h4 += carry3;
        h3 -= carry3 << 25;
        carry7 = (h7 + (long) (1 << 24)) >> 25;
        h8 += carry7;
        h7 -= carry7 << 25;

        carry4 = (h4 + (long) (1 << 25)) >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;
        carry8 = (h8 + (long) (1 << 25)) >> 26;
        h9 += carry8;
        h8 -= carry8 << 26;

        carry9 = (h9 + (long) (1 << 24)) >> 25;
        h0 += carry9 * 19;
        h9 -= carry9 << 25;

        carry0 = (h0 + (long) (1 << 25)) >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;

        z[0] = (int) h0;
        z[1] = (int) h1;
        z[2] = (int) h2;
        z[3] = (int) h3;
        z[4] = (int) h4;
        z[5] = (int) h5;
        z[6] = (int) h6;
        z[7] = (int) h7;
        z[8] = (int) h8;
        z[9] = (int) h9;
    }

    /**
     * Compute z = 2x^2 = 2 * x * x.
     *
     * @param x the input x.
     * @param z the output z = 2x^2.
     */
    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    public static void sqrDbl(int[] x, int[] z) {
        int f0 = x[0];
        int f1 = x[1];
        int f2 = x[2];
        int f3 = x[3];
        int f4 = x[4];
        int f5 = x[5];
        int f6 = x[6];
        int f7 = x[7];
        int f8 = x[8];
        int f9 = x[9];
        int f0_2 = 2 * f0;
        int f1_2 = 2 * f1;
        int f2_2 = 2 * f2;
        int f3_2 = 2 * f3;
        int f4_2 = 2 * f4;
        int f5_2 = 2 * f5;
        int f6_2 = 2 * f6;
        int f7_2 = 2 * f7;
        // 1.959375*2^30
        int f5_38 = 38 * f5;
        // 1.959375*2^30
        int f6_19 = 19 * f6;
        // 1.959375*2^30
        int f7_38 = 38 * f7;
        // 1.959375*2^30
        int f8_19 = 19 * f8;
        // 1.959375*2^30
        int f9_38 = 38 * f9;

        long f0f0 = f0 * (long) f0;
        long f0f1_2 = f0_2 * (long) f1;
        long f0f2_2 = f0_2 * (long) f2;
        long f0f3_2 = f0_2 * (long) f3;
        long f0f4_2 = f0_2 * (long) f4;
        long f0f5_2 = f0_2 * (long) f5;
        long f0f6_2 = f0_2 * (long) f6;
        long f0f7_2 = f0_2 * (long) f7;
        long f0f8_2 = f0_2 * (long) f8;
        long f0f9_2 = f0_2 * (long) f9;
        long f1f1_2 = f1_2 * (long) f1;
        long f1f2_2 = f1_2 * (long) f2;
        long f1f3_4 = f1_2 * (long) f3_2;
        long f1f4_2 = f1_2 * (long) f4;
        long f1f5_4 = f1_2 * (long) f5_2;
        long f1f6_2 = f1_2 * (long) f6;
        long f1f7_4 = f1_2 * (long) f7_2;
        long f1f8_2 = f1_2 * (long) f8;
        long f1f9_76 = f1_2 * (long) f9_38;
        long f2f2 = f2 * (long) f2;
        long f2f3_2 = f2_2 * (long) f3;
        long f2f4_2 = f2_2 * (long) f4;
        long f2f5_2 = f2_2 * (long) f5;
        long f2f6_2 = f2_2 * (long) f6;
        long f2f7_2 = f2_2 * (long) f7;
        long f2f8_38 = f2_2 * (long) f8_19;
        long f2f9_38 = f2 * (long) f9_38;
        long f3f3_2 = f3_2 * (long) f3;
        long f3f4_2 = f3_2 * (long) f4;
        long f3f5_4 = f3_2 * (long) f5_2;
        long f3f6_2 = f3_2 * (long) f6;
        long f3f7_76 = f3_2 * (long) f7_38;
        long f3f8_38 = f3_2 * (long) f8_19;
        long f3f9_76 = f3_2 * (long) f9_38;
        long f4f4 = f4 * (long) f4;
        long f4f5_2 = f4_2 * (long) f5;
        long f4f6_38 = f4_2 * (long) f6_19;
        long f4f7_38 = f4 * (long) f7_38;
        long f4f8_38 = f4_2 * (long) f8_19;
        long f4f9_38 = f4 * (long) f9_38;
        long f5f5_38 = f5 * (long) f5_38;
        long f5f6_38 = f5_2 * (long) f6_19;
        long f5f7_76 = f5_2 * (long) f7_38;
        long f5f8_38 = f5_2 * (long) f8_19;
        long f5f9_76 = f5_2 * (long) f9_38;
        long f6f6_19 = f6 * (long) f6_19;
        long f6f7_38 = f6 * (long) f7_38;
        long f6f8_38 = f6_2 * (long) f8_19;
        long f6f9_38 = f6 * (long) f9_38;
        long f7f7_38 = f7 * (long) f7_38;
        long f7f8_38 = f7_2 * (long) f8_19;
        long f7f9_76 = f7_2 * (long) f9_38;
        long f8f8_19 = f8 * (long) f8_19;
        long f8f9_38 = f8 * (long) f9_38;
        long f9f9_38 = f9 * (long) f9_38;
        long h0 = f0f0 + f1f9_76 + f2f8_38 + f3f7_76 + f4f6_38 + f5f5_38;
        long h1 = f0f1_2 + f2f9_38 + f3f8_38 + f4f7_38 + f5f6_38;
        long h2 = f0f2_2 + f1f1_2 + f3f9_76 + f4f8_38 + f5f7_76 + f6f6_19;
        long h3 = f0f3_2 + f1f2_2 + f4f9_38 + f5f8_38 + f6f7_38;
        long h4 = f0f4_2 + f1f3_4 + f2f2 + f5f9_76 + f6f8_38 + f7f7_38;
        long h5 = f0f5_2 + f1f4_2 + f2f3_2 + f6f9_38 + f7f8_38;
        long h6 = f0f6_2 + f1f5_4 + f2f4_2 + f3f3_2 + f7f9_76 + f8f8_19;
        long h7 = f0f7_2 + f1f6_2 + f2f5_2 + f3f4_2 + f8f9_38;
        long h8 = f0f8_2 + f1f7_4 + f2f6_2 + f3f5_4 + f4f4 + f9f9_38;
        long h9 = f0f9_2 + f1f8_2 + f2f7_2 + f3f6_2 + f4f5_2;

        long carry0;
        long carry1;
        long carry2;
        long carry3;
        long carry4;
        long carry5;
        long carry6;
        long carry7;
        long carry8;
        long carry9;

        h0 += h0;
        h1 += h1;
        h2 += h2;
        h3 += h3;
        h4 += h4;
        h5 += h5;
        h6 += h6;
        h7 += h7;
        h8 += h8;
        h9 += h9;

        carry0 = (h0 + (long) (1 << 25)) >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;
        carry4 = (h4 + (long) (1 << 25)) >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;

        carry1 = (h1 + (long) (1 << 24)) >> 25;
        h2 += carry1;
        h1 -= carry1 << 25;
        carry5 = (h5 + (long) (1 << 24)) >> 25;
        h6 += carry5;
        h5 -= carry5 << 25;

        carry2 = (h2 + (long) (1 << 25)) >> 26;
        h3 += carry2;
        h2 -= carry2 << 26;
        carry6 = (h6 + (long) (1 << 25)) >> 26;
        h7 += carry6;
        h6 -= carry6 << 26;

        carry3 = (h3 + (long) (1 << 24)) >> 25;
        h4 += carry3;
        h3 -= carry3 << 25;
        carry7 = (h7 + (long) (1 << 24)) >> 25;
        h8 += carry7;
        h7 -= carry7 << 25;

        carry4 = (h4 + (long) (1 << 25)) >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;
        carry8 = (h8 + (long) (1 << 25)) >> 26;
        h9 += carry8;
        h8 -= carry8 << 26;

        carry9 = (h9 + (long) (1 << 24)) >> 25;
        h0 += carry9 * 19;
        h9 -= carry9 << 25;

        carry0 = (h0 + (long) (1 << 25)) >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;

        z[0] = (int) h0;
        z[1] = (int) h1;
        z[2] = (int) h2;
        z[3] = (int) h3;
        z[4] = (int) h4;
        z[5] = (int) h5;
        z[6] = (int) h6;
        z[7] = (int) h7;
        z[8] = (int) h8;
        z[9] = (int) h9;
    }

    /**
     * Compute z = x^{-1}.
     *
     * @param x the input x.
     * @param z the output z = 2x^2.
     */
    @SuppressWarnings("AlibabaUndefineMagicConstant")
    public static void inv(int[] x, int[] z) {
        int[] t0 = createZero();
        int[] t1 = createZero();
        int[] t2 = createZero();
        int[] t3 = createZero();
        int i;

        // 2 == 2 * 1, t0 = sqr()
        sqr(x, t0);
        // 4 == 2 * 2, t1 = t0.sqr()
        sqr(t0, t1);
        // 8 == 2 * 4, t1 = t1.sqr()
        sqr(t1, t1);
        // 9 == 8 + 1, t1 = mul(t1)
        mul(x, t1, t1);
        // 11 == 9 + 2, t0 = t0.mul(t1)
        mul(t0, t1, t0);
        // 22 == 2 * 11, t2 = t0.sqr()
        sqr(t0, t2);
        // 31 == 22 + 9, t1 = t1.mul(t2)
        mul(t1, t2, t1);
        // 2^6 - 2^1, t2 = t1.sqr()
        sqr(t1, t2);
        // 2^10 - 2^5
        for (i = 1; i < 5; ++i) {
            // t2 = t2.sqr()
            sqr(t2, t2);
        }
        // 2^10 - 2^0, t1 = t2.mul(t1)
        mul(t2, t1, t1);
        // 2^11 - 2^1, t2 = t1.sqr()
        sqr(t1, t2);
        // 2^20 - 2^10
        for (i = 1; i < 10; ++i) {
            // t2 = t2.sqr()
            sqr(t2, t2);
        }
        // 2^20 - 2^0, t2 = t2.mul(t1)
        mul(t2, t1, t2);
        // 2^21 - 2^1, t3 = t2.sqr()
        sqr(t2, t3);
        // 2^40 - 2^20
        for (i = 1; i < 20; ++i) {
            // t3 = t3.sqr()
            sqr(t3, t3);
        }
        // 2^40 - 2^0, t2 = t3.mul(t2)
        mul(t3, t2, t2);
        // 2^41 - 2^1, t2 = t2.sqr()
        sqr(t2, t2);
        // 2^50 - 2^10
        for (i = 1; i < 10; ++i) {
            // t2 = t2.sqr()
            sqr(t2, t2);
        }
        // 2^50 - 2^0, t1 = t2.mul(t1)
        mul(t2, t1, t1);
        // 2^51 - 2^1, t2 = t1.sqr()
        sqr(t1, t2);
        // 2^100 - 2^50
        for (i = 1; i < 50; ++i) {
            // t2 = t2.sqr()
            sqr(t2, t2);
        }
        // 2^100 - 2^0, t2 = t2.mul(t1)
        mul(t2, t1, t2);
        // 2^101 - 2^1, t3 = t2.sqr()
        sqr(t2, t3);
        // 2^200 - 2^100
        for (i = 1; i < 100; ++i) {
            // t3 = t3.sqr()
            sqr(t3, t3);
        }
        // 2^200 - 2^0, t2 = t3.mul(t2)
        mul(t3, t2, t2);
        // 2^201 - 2^1, t2 = t2.sqr()
        sqr(t2, t2);
        // 2^250 - 2^50
        for (i = 1; i < 50; ++i) {
            // t2 = t2.sqr()
            sqr(t2, t2);
        }
        // 2^250 - 2^0, t1 = t2.mul(t1)
        mul(t2, t1, t1);
        // 2^251 - 2^1, t1 = t1.sqr()
        sqr(t1, t1);
        // 2^255 - 2^5
        for (i = 1; i < 5; ++i) {
            // t1 = t1.sqr()
            sqr(t1, t1);
        }
        // 2^255 - 21, return t1.mul(t0)
        mul(t1, t0, z);
    }

    /**
     * Compute z = x^{(p - 5) / 8} = x^{2^{252} - 3}.
     *
     * @param x the input x.
     * @param z the output z = x^{(p - 5) / 8}.
     */
    @SuppressWarnings("AlibabaUndefineMagicConstant")
    public static void powPm5d8(int[] x, int[] z) {
        int[] t0 = createZero();
        int[] t1 = createZero();
        int[] t2 = createZero();
        int i;

        // 2 == 2 * 1, t0 = sqr()
        sqr(x, t0);
        // 4 == 2 * 2， t1 = t0.sqr()
        sqr(t0, t1);
        // 8 == 2 * 4, t1 = t1.sqr()
        sqr(t1, t1);
        // z9 = z1 * z8, t1 = mul(t1);
        mul(x, t1, t1);
        // 11 == 9 + 2, t0 = t0.mul(t1)
        mul(t0, t1, t0);
        // 22 == 2 * 11, t0 = t0.sqr()
        sqr(t0, t0);
        // 31 == 22 + 9, t0 = t1.mul(t0)
        mul(t1, t0, t0);
        // 2^6 - 2^1, t1 = t0.sqr()
        sqr(t0, t1);
        // 2^10 - 2^5
        for (i = 1; i < 5; ++i) {
            // t1 = t1.sqr()
            sqr(t1, t1);
        }
        // 2^10 - 2^0, t0 = t1.mul(t0)
        mul(t1, t0, t0);
        // 2^11 - 2^1, t1 = t0.sqr()
        sqr(t0, t1);
        // 2^20 - 2^10
        for (i = 1; i < 10; ++i) {
            // t1 = t1.sqr()
            sqr(t1, t1);
        }
        // 2^20 - 2^0, t1 = t1.mul(t0)
        mul(t1, t0, t1);
        // 2^21 - 2^1, t2 = t1.sqr()
        sqr(t1, t2);
        // 2^40 - 2^20
        for (i = 1; i < 20; ++i) {
            // t2 = t2.sqr()
            sqr(t2, t2);
        }
        // 2^40 - 2^0, t1 = t2.mul(t1)
        mul(t2, t1, t1);
        // 2^41 - 2^1, t1 = t1.sqr()
        sqr(t1, t1);
        // 2^50 - 2^10
        for (i = 1; i < 10; ++i) {
            // t1 = t1.sqr()
            sqr(t1, t1);
        }
        // 2^50 - 2^0, t0 = t1.mul(t0)
        mul(t1, t0, t0);
        // 2^51 - 2^1, t1 = t0.sqr()
        sqr(t0, t1);
        // 2^100 - 2^50
        for (i = 1; i < 50; ++i) {
            // t1 = t1.sqr()
            sqr(t1, t1);
        }
        // 2^100 - 2^0, t1 = t1.mul(t0)
        mul(t1, t0, t1);
        // 2^101 - 2^1, t2 = t1.sqr()
        sqr(t1, t2);
        // 2^200 - 2^100
        for (i = 1; i < 100; ++i) {
            // t2 = t2.sqr()
            sqr(t2, t2);
        }
        // 2^200 - 2^0, t1 = t2.mul(t1)
        mul(t2, t1, t1);
        // 2^201 - 2^1, t1 = t1.sqr()
        sqr(t1, t1);
        // 2^250 - 2^50
        for (i = 1; i < 50; ++i) {
            // t1 = t1.sqr()
            sqr(t1, t1);
        }
        // 2^250 - 2^0, t0 = t1.mul(t0)
        mul(t1, t0, t0);
        // 2^251 - 2^1, t0 = t0.sqr()
        sqr(t0, t0);
        // 2^252 - 2^2, t0 = t0.sqr()
        sqr(t0, t0);
        // 2^252 - 3, return mul(t0)
        mul(x, t0, z);
    }

    /**
     * Compute z = x^{(p - 1) / 2}.
     *
     * @param x the input x.
     * @param z the output z = x^{(p - 1) / 2}.
     */
    @SuppressWarnings("AlibabaUndefineMagicConstant")
    public static void chi(int[] x, int[] z) {
        // var t0, t1, t2, t3 edwards25519.FieldElement
        int[] t0 = createZero();
        int[] t1 = createZero();
        int[] t2 = createZero();
        int[] t3 = createZero();
        int i;
        // edwards25519.FeSquare(&t0, z)     // 2^1
        sqr(x, t0);
        // edwards25519.FeMul(&t1, &t0, z)   // 2^1 + 2^0
        mul(t0, x, t1);
        // edwards25519.FeSquare(&t0, &t1)   // 2^2 + 2^1
        sqr(t1, t0);
        // edwards25519.FeSquare(&t2, &t0)   // 2^3 + 2^2
        sqr(t0, t2);
        // edwards25519.FeSquare(&t2, &t2)   // 4,3
        sqr(t2, t2);
        // edwards25519.FeMul(&t2, &t2, &t0) // 4,3,2,1
        mul(t2, t0, t2);
        // edwards25519.FeMul(&t1, &t2, z)   // 4..0
        mul(t2, x, t1);
        // edwards25519.FeSquare(&t2, &t1)   // 5..1
        sqr(t1, t2);
        // 9,8,7,6,5
        for (i = 1; i < 5; i++) {
            // edwards25519.FeSquare(&t2, &t2)
            sqr(t2, t2);
        }
        // edwards25519.FeMul(&t1, &t2, &t1) // 9,8,7,6,5,4,3,2,1,0
        mul(t2, t1, t1);
        // edwards25519.FeSquare(&t2, &t1)   // 10..1
        sqr(t1, t2);
        // 19..10
        for (i = 1; i < 10; i++) {
            // edwards25519.FeSquare(&t2, &t2)
            sqr(t2, t2);
        }
        // edwards25519.FeMul(&t2, &t2, &t1) // 19..0
        mul(t2, t1, t2);
        // edwards25519.FeSquare(&t3, &t2)   // 20..1
        sqr(t2, t3);
        // 39..20
        for (i = 1; i < 20; i++) {
            // edwards25519.FeSquare(&t3, &t3)
            sqr(t3, t3);
        }
        // edwards25519.FeMul(&t2, &t3, &t2) // 39..0
        mul(t3, t2, t2);
        // edwards25519.FeSquare(&t2, &t2)   // 40..1
        sqr(t2, t2);
        // 49..10
        for (i = 1; i < 10; i++) {
            // edwards25519.FeSquare(&t2, &t2)
            sqr(t2, t2);
        }
        // edwards25519.FeMul(&t1, &t2, &t1) // 49..0
        mul(t2, t1, t1);
        // edwards25519.FeSquare(&t2, &t1)   // 50..1
        sqr(t1, t2);
        // 99..50
        for (i = 1; i < 50; i++) {
            // edwards25519.FeSquare(&t2, &t2)
            sqr(t2, t2);
        }
        // edwards25519.FeMul(&t2, &t2, &t1) // 99..0
        mul(t2, t1, t2);
        // edwards25519.FeSquare(&t3, &t2)   // 100..1
        sqr(t2, t3);
        // 199..100
        for (i = 1; i < 100; i++) {
            // edwards25519.FeSquare(&t3, &t3)
            sqr(t3, t3);
        }
        // edwards25519.FeMul(&t2, &t3, &t2) // 199..0
        mul(t3, t2, t2);
        // edwards25519.FeSquare(&t2, &t2)   // 200..1
        sqr(t2, t2);
        // 249..50
        for (i = 1; i < 50; i++) {
            // edwards25519.FeSquare(&t2, &t2)
            sqr(t2, t2);
        }
        // edwards25519.FeMul(&t1, &t2, &t1) // 249..0
        mul(t2, t1, t1);
        // edwards25519.FeSquare(&t1, &t1)   // 250..1
        sqr(t1, t1);
        // 253..4
        for (i = 1; i < 4; i++) {
            // edwards25519.FeSquare(&t1, &t1)
            sqr(t1, t1);
        }
        // edwards25519.FeMul(out, &t1, &t0) // 253..4,2,1
        mul(t1, t0, z);
    }

    /**
     * Decode a field element from the low 255 bits of a 256-bit input.
     *
     * @param x the 32-byte representation.
     * @return the field element.
     */
    public static int[] decode(byte[] x) {
        long h0 = ByteEccUtils.decodeLong32(x, 0);
        long h1 = ByteEccUtils.decodeLong24(x, 4) << 6;
        long h2 = ByteEccUtils.decodeLong24(x, 7) << 5;
        long h3 = ByteEccUtils.decodeLong24(x, 10) << 3;
        long h4 = ByteEccUtils.decodeLong24(x, 13) << 2;
        long h5 = ByteEccUtils.decodeLong32(x, 16);
        long h6 = ByteEccUtils.decodeLong24(x, 20) << 7;
        long h7 = ByteEccUtils.decodeLong24(x, 23) << 5;
        long h8 = ByteEccUtils.decodeLong24(x, 26) << 4;
        long h9 = (ByteEccUtils.decodeLong24(x, 29) & 0x7FFFFF) << 2;
        long carry0;
        long carry1;
        long carry2;
        long carry3;
        long carry4;
        long carry5;
        long carry6;
        long carry7;
        long carry8;
        long carry9;

        // Remember: 2^255 congruent 19 modulo p
        carry9 = (h9 + (long) (1 << 24)) >> 25;
        h0 += carry9 * 19;
        h9 -= carry9 << 25;
        carry1 = (h1 + (long) (1 << 24)) >> 25;
        h2 += carry1;
        h1 -= carry1 << 25;
        carry3 = (h3 + (long) (1 << 24)) >> 25;
        h4 += carry3;
        h3 -= carry3 << 25;
        carry5 = (h5 + (long) (1 << 24)) >> 25;
        h6 += carry5;
        h5 -= carry5 << 25;
        carry7 = (h7 + (long) (1 << 24)) >> 25;
        h8 += carry7;
        h7 -= carry7 << 25;

        carry0 = (h0 + (long) (1 << 25)) >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;
        carry2 = (h2 + (long) (1 << 25)) >> 26;
        h3 += carry2;
        h2 -= carry2 << 26;
        carry4 = (h4 + (long) (1 << 25)) >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;
        carry6 = (h6 + (long) (1 << 25)) >> 26;
        h7 += carry6;
        h6 -= carry6 << 26;
        carry8 = (h8 + (long) (1 << 25)) >> 26;
        h9 += carry8;
        h8 -= carry8 << 26;

        int[] h = createZero();
        h[0] = (int) h0;
        h[1] = (int) h1;
        h[2] = (int) h2;
        h[3] = (int) h3;
        h[4] = (int) h4;
        h[5] = (int) h5;
        h[6] = (int) h6;
        h[7] = (int) h7;
        h[8] = (int) h8;
        h[9] = (int) h9;

        return h;
    }

    /**
     * Ed25519 extension point representation.
     * <p>
     * Given the Ed25519 affine representation (x, y), we have (X, Y, Z, T)，x = X / Z, y = Y / Z, T = XY / Z, Z ≠ 0
     * </p>
     */
    private static class EdwardsPointExt {
        /**
         * coordinate x
         */
        private final int[] x = createZero();
        /**
         * coordinate y
         */
        private int[] y = createZero();
        /**
         * coordinate z
         */
        private int[] z = createZero();
        /**
         * coordinate t
         */
        private final int[] t = createZero();
    }

    /**
     * Decode an Ed25519 extension representation from its compressed representation.
     * <p>
     * See https://github.com/osu-crypto/MiniPSI/blob/master/libPSI/PsiDefines.h, line 1835 for details.
     * </p>
     *
     * @param y the compressed representation.
     * @return the Ed25519 extension representation.
     */
    private static EdwardsPointExt decodeCompress(byte[] y) {
        // ropo_fe25519 u, v, v3, vxx, m_root_check, p_root_check, negx, x_sqrtm1
        int[] u = createZero();
        int[] v = createZero();
        int[] v3 = createZero();
        int[] vxx = createZero();
        int[] mRootCheck = createZero();
        int[] pRootCheck = createZero();
        int[] negX = createZero();
        int[] xSqrtM1 = createZero();
        int mHasRoot, pHasRoot;

        EdwardsPointExt pointExt = new EdwardsPointExt();
        // decode x-coordinate, ropo_fe25519_frombytes(h->Y, s)
        pointExt.y = decode(y);
        // z = 1, ropo_fe25519_1(h->Z)
        pointExt.z = createOne();
        // y^2, ropo_fe25519_sq(u, h->Y)
        sqr(pointExt.y, u);
        // dy^2, ropo_fe25519_mul(v, u, d)
        mul(u, EDWARDS_D_INTS, v);
        // u = y^2 - 1, ropo_fe25519_sub(u, u, h->Z)
        sub(u, pointExt.z, u);
        // v = dy^2 + 1ropo_fe25519_add(v, v, h->Z)
        add(v, pointExt.z, v);

        // v^3, ropo_fe25519_sq(v3, v)
        sqr(v, v3);
        // ropo_fe25519_mul(v3, v3, v) /* v3 = v^3 */
        mul(v3, v, v3);
        // v^6 ropo_fe25519_sq(h->X, v3)
        sqr(v3, pointExt.x);
        // v^7 ropo_fe25519_mul(h->X, h->X, v)
        mul(pointExt.x, pointExt.x, v);
        // uv^7, ropo_fe25519_mul(h->X, h->X, u)
        mul(pointExt.x, u, pointExt.x);

        // (uv^7)^((q-5)/8), ropo_fe25519_pow22523(h->X, h->X)
        powPm5d8(pointExt.x, pointExt.x);
        // v^3(uv^7)^((q-5)/8), ropo_fe25519_mul(h->X, h->X, v3)
        mul(pointExt.x, v3, pointExt.x);
        // x = uv^3(uv^7)^((q-5)/8), ropo_fe25519_mul(h->X, h->X, u)
        mul(pointExt.x, u, pointExt.x);

        // x^2, ropo_fe25519_sq(vxx, h->X)
        sqr(pointExt.x, vxx);
        // vx^2, ropo_fe25519_mul(vxx, vxx, v)
        mul(vxx, v, vxx);
        // vx^2-u, ropo_fe25519_sub(m_root_check, vxx, u)
        sub(vxx, u, mRootCheck);
        // vx^2+u, ropo_fe25519_add(p_root_check, vxx, u)
        add(vxx, u, pRootCheck);
        // has_m_root = ropo_fe25519_iszero(m_root_check)
        mHasRoot = isZero(mRootCheck);
        // has_p_root = ropo_fe25519_iszero(p_root_check)
        pHasRoot = isZero(pRootCheck);
        // x * sqrt(-1), ropo_fe25519_mul(x_sqrtm1, h->X, sqrtm1)
        mul(pointExt.x, SQRT_M1_INTS, xSqrtM1);
        // ropo_fe25519_cmov(h->X, x_sqrtm1, 1 - has_m_root)
        cmov(mHasRoot ^ 1, xSqrtM1, pointExt.x);

        // ropo_fe25519_neg(negx, h->X)
        neg(pointExt.x, negX);
        // ropo_fe25519_cmov(h->X, negx, ropo_fe25519_isnegative(h->X) ^ (s[31] >> 7))
        cmov(isNeg(pointExt.x) ^ (y[31] >> 7), negX, pointExt.x);
        // ropo_fe25519_mul(h->T, h->X, h->Y)
        mul(pointExt.x, pointExt.y, pointExt.t);

        if (((mHasRoot | pHasRoot) ^ 1) == 0) {
            throw new IllegalArgumentException("Invalid x-coordinate montgomery representation.");
        }

        return pointExt;
    }

    /**
     * Edwards Elligator encode Edwards point representation.
     *
     * @param y            Edwards compressed point representation.
     * @param point        encode representation.
     * @param uniformPoint uniform encode representation.
     * @return true if success, false otherwise.
     */
    public static boolean elligatorEncode(byte[] y, byte[] point, byte[] uniformPoint) {
        if (y.length != Ed25519ByteEccUtils.POINT_BYTES) {
            throw new IllegalArgumentException("Invalid Edwards byte[] representation");
        }
        EdwardsPointExt repr = decodeCompress(y);
        // var inv1 edwards25519.FieldElement
        int[] inv1 = createZero();
        // edwards25519.FeSub(&inv1, &A.Z, &A.Y)
        sub(repr.z, repr.y, inv1);
        // edwards25519.FeMul(&inv1, &inv1, &A.X)
        mul(inv1, repr.x, inv1);
        // edwards25519.FeInvert(&inv1, &inv1)
        inv(inv1, inv1);

        // var t0, u edwards25519.FieldElement
        int[] t0 = createZero();
        int[] u = createZero();
        // edwards25519.FeMul(&u, &inv1, &A.X)
        mul(inv1, repr.x, u);
        // edwards25519.FeAdd(&t0, &A.Y, &A.Z)
        add(repr.y, repr.z, t0);
        // edwards25519.FeMul(&u, &u, &t0)
        mul(u, t0, u);

        // var v edwards25519.FieldElement
        int[] v = createZero();
        // edwards25519.FeMul(&v, &t0, &inv1)
        mul(t0, inv1, v);
        // edwards25519.FeMul(&v, &v, &A.Z)
        mul(v, repr.z, v);
        // edwards25519.FeMul(&v, &v, &sqrtMinusAPlus2)
        mul(v, SQRT_MINUS_A_PLUS_2_INTS, v);

        // var b edwards25519.FieldElement
        int[] b = createZero();
        // edwards25519.FeAdd(&b, &u, &edwards25519.A)
        add(u, A_INTS, b);

        // var c, b3, b7, b8 edwards25519.FieldElement
        int[] c = createZero();
        int[] b3 = createZero();
        int[] b7 = createZero();
        int[] b8 = createZero();
        // edwards25519.FeSquare(&b3, &b) // 2
        sqr(b, b3);
        // edwards25519.FeMul(&b3, &b3, &b) // 3
        mul(b3, b, b3);
        // edwards25519.FeSquare(&c, &b3) // 6
        sqr(b3, c);
        // edwards25519.FeMul(&b7, &c, &b)  // 7
        mul(c, b, b7);
        // edwards25519.FeMul(&b8, &b7, &b) // 8
        mul(b7, b, b8);
        // edwards25519.FeMul(&c, &b7, &u)
        mul(b7, u, c);
        // q58(&c, &c)
        powPm5d8(c, c);

        // var chi edwards25519.FieldElement
        int[] chi = createZero();
        // edwards25519.FeSquare(&chi, &c)
        sqr(c, chi);
        // edwards25519.FeSquare(&chi, &chi)
        sqr(chi, chi);

        // edwards25519.FeSquare(&t0, &u)
        sqr(u, t0);
        // edwards25519.FeMul(&chi, &chi, &t0)
        mul(chi, t0, chi);
        // edwards25519.FeSquare(&t0, &b7) // 14
        sqr(b7, t0);
        // edwards25519.FeMul(&chi, &chi, &t0)
        mul(chi, t0, chi);
        // edwards25519.FeNeg(&chi, &chi)
        neg(chi, chi);

        // edwards25519.FeToBytes(&chiBytes, &chi)
        byte[] chiBytes = encode(chi);
        // chi[1] is either 0 or 0xff
        if (chiBytes[1] == (byte) 0xFF) {
            return false;
        }

        // Calculate r1 = sqrt(-u/(2*(u+A)))
        // var r1 edwards25519.FieldElement
        int[] r1 = createZero();
        // edwards25519.FeMul(&r1, &c, &u)
        mul(c, u, r1);
        // edwards25519.FeMul(&r1, &r1, &b3)
        mul(r1, b3, r1);
        // edwards25519.FeMul(&r1, &r1, &sqrtMinusHalf)
        mul(r1, SQRT_MINUS_HALF_INTS, r1);

        // edwards25519.FeSquare(&t0, &r1)
        sqr(r1, t0);
        // edwards25519.FeMul(&t0, &t0, &b)
        mul(t0, b, t0);
        // edwards25519.FeAdd(&t0, &t0, &t0)
        add(t0, t0, t0);
        // edwards25519.FeAdd(&t0, &t0, &u)
        add(t0, u, t0);

        // edwards25519.FeOne(&maybeSqrtM1)
        int[] maybeSqrtM1 = createOne();
        // edwards25519.FeCMove(&maybeSqrtM1, &edwards25519.SqrtM1, edwards25519.FeIsNonZero(&t0))
        cmov(areEqual(t0, ZERO_INTS) ^ 1, SQRT_M1_INTS, maybeSqrtM1);
        // edwards25519.FeMul(&r1, &r1, &maybeSqrtM1)
        mul(r1, maybeSqrtM1, r1);

        // Calculate r = sqrt(-(u+A)/(2u))
        // var r edwards25519.FieldElement
        int[] r = createZero();
        // edwards25519.FeSquare(&t0, &c) // 2
        sqr(c, t0);
        // edwards25519.FeMul(&t0, &t0, &c) // 3
        mul(t0, c, t0);
        // edwards25519.FeSquare(&t0, &t0) // 6
        sqr(t0, t0);
        // edwards25519.FeMul(&r, &t0, &c) // 7
        mul(t0, c, r);

        // edwards25519.FeSquare(&t0, &u) // 2
        sqr(u, t0);
        // edwards25519.FeMul(&t0, &t0, &u) // 3
        mul(t0, u, t0);
        // edwards25519.FeMul(&r, &r, &t0)
        mul(r, t0, r);

        // edwards25519.FeSquare(&t0, &b8) // 16
        sqr(b8, t0);
        // edwards25519.FeMul(&t0, &t0, &b8) // 24
        mul(t0, b8, t0);
        // edwards25519.FeMul(&t0, &t0, &b) // 25
        mul(t0, b, t0);
        // edwards25519.FeMul(&r, &r, &t0)
        mul(r, t0, r);
        // edwards25519.FeMul(&r, &r, &sqrtMinusHalf)
        mul(r, SQRT_MINUS_HALF_INTS, r);

        // edwards25519.FeSquare(&t0, &r)
        sqr(r, t0);
        // edwards25519.FeMul(&t0, &t0, &u)
        mul(t0, u, t0);
        // edwards25519.FeAdd(&t0, &t0, &t0)
        add(t0, t0, t0);
        // edwards25519.FeAdd(&t0, &t0, &b)
        add(t0, b, t0);
        // edwards25519.FeOne(&maybeSqrtM1)
        maybeSqrtM1 = createOne();
        // edwards25519.FeCMove(&maybeSqrtM1, &edwards25519.SqrtM1, edwards25519.FeIsNonZero(&t0))
        cmov(areEqual(t0, ZERO_INTS) ^ 1, SQRT_M1_INTS, maybeSqrtM1);
        // edwards25519.FeMul(&r, &r, &maybeSqrtM1)
        mul(r, maybeSqrtM1, r);

        // vInSquareRootImage := feBytesLE(&vBytes, &halfQMinus1Bytes)
        int vInSquareRootImage = isNeg(v) ^ 1;
        // edwards25519.FeCMove(&r, &r1, vInSquareRootImage)
        cmov(vInSquareRootImage, r1, r);

        // edwards25519.FeToBytes(publicKey, &u)
        byte[] uBytes = encode(u);
        System.arraycopy(uBytes, 0, point, 0, BYTE_SIZE);

        // edwards25519.FeToBytes(representative, &r)
        byte[] rBytes = encode(r);
        System.arraycopy(rBytes, 0, uniformPoint, 0, BYTE_SIZE);

        return true;
    }

    /**
     * Edwards Elligator decode uniform point representation.
     *
     * @param uniformPoint uniform encode representation.
     * @return encode representation.
     */
    public static byte[] elligatorDecode(byte[] uniformPoint) {
        if (uniformPoint.length != Ed25519ByteEccUtils.POINT_BYTES) {
            throw new IllegalArgumentException("Invalid Edwards uniform byte[] representation");
        }
        // var rr2, v, e edwards25519.FieldElement
        int[] v = createZero();
        int[] e = createZero();

        // edwards25519.FeFromBytes(&rr2, representative)
        int[] rr2 = decode(uniformPoint);
        // compute d = -A / (1 + 2r^2)
        // edwards25519.FeSquare2(&rr2, &rr2)
        sqrDbl(rr2, rr2);
        // rr2[0]++
        rr2[0]++;
        // edwards25519.FeInvert(&rr2, &rr2)
        inv(rr2, rr2);
        // edwards25519.FeMul(&v, &edwards25519.A, &rr2)
        mul(A_INTS, rr2, v);
        // edwards25519.FeNeg(&v, &v)
        neg(v, v);

        // compute e = (d^3 + Ad^2 + d)^{(q - 1) / 2}
        // var v2, v3 edwards25519.FieldElement
        int[] v2 = createZero();
        int[] v3 = createZero();
        // edwards25519.FeSquare(&v2, &v)
        sqr(v, v2);
        // edwards25519.FeMul(&v3, &v, &v2)
        mul(v2, v, v3);
        // edwards25519.FeAdd(&e, &v3, &v)
        add(v3, v, e);
        // edwards25519.FeMul(&v2, &v2, &edwards25519.A)
        mul(v2, A_INTS, v2);
        // edwards25519.FeAdd(&e, &v2, &e)
        add(e, v2, e);
        // chi(&e, &e)
        chi(e, e);
        // edwards25519.FeToBytes(&eBytes, &e)
        byte[] eBytes = encode(e);
        // eBytes[1] is either 0 (for e = 1) or 0xff (for e = -1)
        // eIsMinus1 := int32(eBytes[1]) & 1
        int eIsMinus1 = ((int)eBytes[1]) & 1;
        // var negV edwards25519.FieldElement
        int[] negV = createZero();
        // edwards25519.FeNeg(&negV, &v)
        neg(v, negV);
        // edwards25519.FeCMove(&v, &negV, eIsMinus1)
        cmov(eIsMinus1, negV, v);
        // edwards25519.FeZero(&v2)
        v2 = createZero();
        // edwards25519.FeCMove(&v2, &edwards25519.A, eIsMinus1)
        cmov(eIsMinus1, A_INTS, v2);
        // edwards25519.FeSub(&v, &v, &v2)
        sub(v, v2, v);
        return encode(v);
    }
}
