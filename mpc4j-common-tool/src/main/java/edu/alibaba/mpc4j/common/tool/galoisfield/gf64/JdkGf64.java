package edu.alibaba.mpc4j.common.tool.galoisfield.gf64;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64Factory.Gf64Type;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.bouncycastle.util.Longs;
import org.bouncycastle.util.Pack;

/**
 * GF(2^64) using pure Java.
 *
 * @author Weiran Liu
 * @date 2023/8/28
 */
class JdkGf64 extends AbstractGf64 {

    public JdkGf64(EnvType envType) {
        super(envType);
    }

    @Override
    public Gf64Type getGf64Type() {
        return Gf64Type.JDK;
    }

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        byte[] r = BytesUtils.clone(p);
        muli(r, q);
        return r;
    }

    @Override
    public void muli(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        long x = asLong(p);
        long y = asLong(q);
        // 1. carry-less multiplication
        // we use rev(x) * rev(y) == rev((x * y) << 1) to compute the high part of a 64x64 product x * y,
        // where rev(·) is the bit reversal of the input.
        long xr = Longs.reverse(x);
        long yr = Longs.reverse(y);
        // [X1 : X0] = A0 ● B0
        long z1 = Long.reverse(implMul64(xr, yr) << 1);
        long z0 = implMul64(x, y);
        // 2. reduction modulo P(X) = X^64 + X^4 + X^3 + X + 1
        // Denote the input operand by [X1 : X0] where X1 and X0 are 64 bit long each.
        // Step 1: shift X1 by 63, 61 and 60-bit positions to the right, i.e., compute:
        //  A = X1 >> 63, B = X1 >> 61, C = X1 >> 60
        long a = z1 >>> 63, b = z1 >>> 61, c = z1 >>> 60;
        // Step 2: We XOR A, B, and C with X1, i.e., compute a number D as follows: D = A ⊕ B ⊕ C ⊕ X1
        long h0 = z1 ^ a ^ b ^ c;
        // E0 = D << 1, F0 = D << 3, G0 = D << 4
        long e0 = h0 << 1;
        long f0 = h0 << 3;
        long g0 = h0 << 4;
        // Step 4: XOR E0, F0, and G0 with each other and D, i.e., compute the following:
        // H0 = D ⊕ E0 ⊕ F0 ⊕ G0
        long i0 = h0 ^ e0 ^ f0 ^ g0;
        // Return X0 ⊕ H0
        x = z0 ^ i0;
        asBytes(x, p);
    }

    @Override
    public byte[] inv(byte[] p) {
        assert validateNonZeroElement(p);
        // The order of GF(2^64) = 2^64. We can calculate p^{-1} as p^{2^{64}-2} so that p^{-1} * p = p^{2^{64}-1} = 1
        // The addition chain below requires 142 mul/sqr operations total.
        byte[] a = BytesUtils.clone(p);
        byte[] r = new byte[BYTE_L];
        for (int i = 0; i <= 5; i++) {
            // entering the loop a = p^{2^{2^i}-1}
            byte[] b = BytesUtils.clone(a);
            for (int j = 0; j < (1 << i); j++) {
                byte[] copyB = BytesUtils.clone(b);
                muli(b, copyB);
            }
            // after the loop b = a^{2^i} = p^{2^{2^i}*(2^{2^i}-1)}
            muli(a, b);
            // now a = x^{2^{2^{i+1}}-1}
            if (i == 0) {
                r = BytesUtils.clone(b);
            } else {
                muli(r, b);
            }
        }
        return r;
    }

    @Override
    public void invi(byte[] p) {
        byte[] y = inv(p);
        System.arraycopy(y, 0, p, 0, BYTE_L);
    }

    @Override
    public byte[] div(byte[] p, byte[] q) {
        byte[] qInv = inv(q);
        return mul(p, qInv);
    }

    @Override
    public void divi(byte[] p, byte[] q) {
        byte[] qInv = inv(q);
        muli(p, qInv);
    }

    /**
     * Implements 64-bit carry-less multiplication and keeps the low 64-it result using long. This code comes from
     * GCMUtil.java in org.bouncycastle.crypto.modes.gcm.
     *
     * @param x x.
     * @param y y.
     * @return low 64-bit of x ● y.
     */
    private long implMul64(long x, long y) {
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

    private long asLong(byte[] x) {
        return Pack.bigEndianToLong(x, 0);
    }

    private void asBytes(long x, byte[] z) {
        Pack.longToBigEndian(x, z, 0);
    }
}
