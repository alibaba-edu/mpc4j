package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * GF(2^16) using JDK.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
class JdkGf016 extends AbstractGf2e {
    /**
     * lookup table for X^i mod x^16 + x^5 + x^3 + x^2 + 1
     */
    static long[] X_POW_MOD_LOOKUP_TABLE = new long[]{
        // X^00, X^01, X^02, X^03
        0b0000000000000001L, 0b0000000000000010L, 0b0000000000000100L, 0b0000000000001000L,
        // X^04, X^05, X^06, X^07
        0b0000000000010000L, 0b0000000000100000L, 0b0000000001000000L, 0b0000000010000000L,
        // X^08, X^09, X^10, X^11
        0b0000000100000000L, 0b0000001000000000L, 0b0000010000000000L, 0b0000100000000000L,
        // X^12, X^13, X^14, X^15
        0b0001000000000000L, 0b0010000000000000L, 0b0100000000000000L, 0b1000000000000000L,
        // X^16, X^17, X^18, X^19
        0b0000000000101101L, 0b0000000001011010L, 0b0000000010110100L, 0b0000000101101000L,
        // X^20, X^21, X^22 X^23
        0b0000001011010000L, 0b0000010110100000L, 0b0000101101000000L, 0b0001011010000000L,
        // X^24, X^25, X^26 X^27
        0b0010110100000000L, 0b0101101000000000L, 0b1011010000000000L, 0b0110100000101101L,
        // X^28, X^29, X^30, X^31
        0b1101000001011010L, 0b1010000010011001L, 0b0100000100011111L, 0b1000001000111110L,
    };

    public JdkGf016(EnvType envType) {
        super(envType, 16);
    }

    @Override
    public Gf2eType getGf2eType() {
        return Gf2eType.JDK;
    }

    @Override
    public boolean validateElement(byte[] p) {
        return p.length == 2;
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
        // carry-less multiplication
        long z = JdkGf2eUtils.implMul64(x, y);
        // reduction modulo P(X), we do that using lookup tables
        for (int i = l; i <= (l - 1) * 2; i++) {
            if ((z & (1L << i)) != 0) {
                z ^= X_POW_MOD_LOOKUP_TABLE[i];
            }
        }
        asBytes(z, p);
    }

    @Override
    public byte[] inv(byte[] p) {
        assert validateNonZeroElement(p);
        // The order of GF(2^16) = 2^16. We can calculate p^{-1} as p^{2^{16}-2} so that p^{-1} * p = p^{2^{16}-1} = 1
        byte[] a = BytesUtils.clone(p);
        byte[] r = new byte[byteL];
        for (int i = 0; i <= 3; i++) {
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
        assert validateNonZeroElement(p);
        // The order of GF(2^16) = 2^16. We can calculate p^{-1} as p^{2^{16}-2} so that p^{-1} * p = p^{2^{16}-1} = 1
        byte[] a = BytesUtils.clone(p);
        for (int i = 0; i <= 3; i++) {
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
                System.arraycopy(b, 0, p, 0, byteL);
            } else {
                muli(p, b);
            }
        }
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

    private long asLong(byte[] x) {
        return ((x[0] & 0b11111111L) << 8) | (x[1] & 0b11111111L);
    }

    private void asBytes(long x, byte[] z) {
        z[0] = (byte) (x >>> 8);
        z[1] = (byte) x;
    }
}