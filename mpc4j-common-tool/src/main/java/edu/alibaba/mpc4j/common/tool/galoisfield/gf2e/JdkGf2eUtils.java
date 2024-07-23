package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

/**
 * JDK GF(2^l) utilities. The source code comes from GCMUtil.java in org.bouncycastle.crypto.modes.gcm.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
class JdkGf2eUtils {
    /**
     * private constructor
     */
    private JdkGf2eUtils() {
        // empty
    }

    /**
     * Implements 64-bit carry-less multiplication and keeps the low 64-bit result using long, where x and y are in the
     * big-endian form, i.e., the MSB of x and y represents the coefficient of X^63, and the LSB of x and y represents
     * the coefficient of X^0.
     *
     * @param x x.
     * @param y y.
     * @return low 64-bit of x ● y.
     */
    static long implMul64(long x, long y) {
        long x0 = x & 0x1111111111111111L;
        long x1 = x & 0x2222222222222222L;
        long x2 = x & 0x4444444444444444L;
        long x3 = x & 0x8888888888888888L;

        long y0 = y & 0x1111111111111111L;
        long y1 = y & 0x2222222222222222L;
        long y2 = y & 0x4444444444444444L;
        long y3 = y & 0x8888888888888888L;

        long z0 = (x0 * y0) ^ (x1 * y3) ^ (x2 * y2) ^ (x3 * y1);
        long z1 = (x0 * y1) ^ (x1 * y0) ^ (x2 * y3) ^ (x3 * y2);
        long z2 = (x0 * y2) ^ (x1 * y1) ^ (x2 * y0) ^ (x3 * y3);
        long z3 = (x0 * y3) ^ (x1 * y2) ^ (x2 * y1) ^ (x3 * y0);

        z0 &= 0x1111111111111111L;
        z1 &= 0x2222222222222222L;
        z2 &= 0x4444444444444444L;
        z3 &= 0x8888888888888888L;

        return z0 | z1 | z2 | z3;
    }
}
