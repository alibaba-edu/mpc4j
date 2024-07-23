package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * GF(2^8) using JDK.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
class JdkGf008 extends AbstractGf2e {
    /**
     * lookup table for X^i mod X^8 + X^4 + X^3 + X^2 + 1
     */
    static long[] X_POW_MOD_LOOKUP_TABLE = new long[]{
        // X^0, X^1, X^2, X^3, X^4, X^5, X^6, X^7
        0b00000001L, 0b00000010L, 0b00000100L, 0b00001000L, 0b00010000L, 0b00100000L, 0b01000000L, 0b10000000L,
        // X^8, X^9, X^10, X^11, X^12, X^13, X^14, X^15
        0b00011101L, 0b00111010L, 0b01110100L, 0b11101000L, 0b11001101L, 0b10000111L, 0b00010011L, 0b00100110L,
    };

    public JdkGf008(EnvType envType) {
        super(envType, 8);
    }

    @Override
    public Gf2eType getGf2eType() {
        return Gf2eType.JDK;
    }

    @Override
    public boolean validateElement(byte[] p) {
        return p.length == 1;
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
        // The order of GF(2^8) = 2^8. We can calculate p^{-1} as p^{2^{8}-2} so that p^{-1} * p = p^{2^{8}-1} = 1
        byte[] a = BytesUtils.clone(p);
        byte[] r = new byte[byteL];
        for (int i = 0; i <= 2; i++) {
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
        // The order of GF(2^8) = 2^8. We can calculate p^{-1} as p^{2^{8}-2} so that p^{-1} * p = p^{2^{8}-1} = 1
        byte[] a = BytesUtils.clone(p);
        for (int i = 0; i <= 2; i++) {
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
        return (x[0] & 0b11111111L);
    }

    private void asBytes(long x, byte[] z) {
        z[0] = (byte) x;
    }
}
