package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * GF(2^32) using JDK.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
class JdkGf032 extends AbstractGf2e {
    /**
     * lookup table for X^i mod x^32 + x^7 + x^6 + x^2 + 1
     */
    static long[] X_POW_MOD_LOOKUP_TABLE = new long[]{
        // X^00, X^01
        0b00000000000000000000000000000001L, 0b00000000000000000000000000000010L,
        // X^02, X^03
        0b00000000000000000000000000000100L, 0b00000000000000000000000000001000L,
        // X^04, X^05
        0b00000000000000000000000000010000L, 0b00000000000000000000000000100000L,
        // X^06, X^07
        0b00000000000000000000000001000000L, 0b00000000000000000000000010000000L,
        // X^08, X^09
        0b00000000000000000000000100000000L, 0b00000000000000000000001000000000L,
        // X^10, X^11
        0b00000000000000000000010000000000L, 0b00000000000000000000100000000000L,
        // X^12, X^13
        0b00000000000000000001000000000000L, 0b00000000000000000010000000000000L,
        // X^14, X^15
        0b00000000000000000100000000000000L, 0b00000000000000001000000000000000L,
        // X^16, X^17
        0b00000000000000010000000000000000L, 0b00000000000000100000000000000000L,
        // X^18, X^19
        0b00000000000001000000000000000000L, 0b00000000000010000000000000000000L,
        // X^20, X^21
        0b00000000000100000000000000000000L, 0b00000000001000000000000000000000L,
        // X^22, X^23
        0b00000000010000000000000000000000L, 0b00000000100000000000000000000000L,
        // X^24, X^25
        0b00000001000000000000000000000000L, 0b00000010000000000000000000000000L,
        // X^26, X^27
        0b00000100000000000000000000000000L, 0b00001000000000000000000000000000L,
        // X^28, X^29
        0b00010000000000000000000000000000L, 0b00100000000000000000000000000000L,
        // X^30, X^31
        0b01000000000000000000000000000000L, 0b10000000000000000000000000000000L,
        // X^32, X^33
        0b00000000000000000000000011000101L, 0b00000000000000000000000110001010L,
        // X^34, X^35
        0b00000000000000000000001100010100L, 0b00000000000000000000011000101000L,
        // X^36, X^37
        0b00000000000000000000110001010000L, 0b00000000000000000001100010100000L,
        // X^38, X^39
        0b00000000000000000011000101000000L, 0b00000000000000000110001010000000L,
        // X^40, X^41
        0b00000000000000001100010100000000L, 0b00000000000000011000101000000000L,
        // X^42, X^43
        0b00000000000000110001010000000000L, 0b00000000000001100010100000000000L,
        // X^44, X^45
        0b00000000000011000101000000000000L, 0b00000000000110001010000000000000L,
        // X^46, X^47
        0b00000000001100010100000000000000L, 0b00000000011000101000000000000000L,
        // X^48, X^49
        0b00000000110001010000000000000000L, 0b00000001100010100000000000000000L,
        // X^50, X^51
        0b00000011000101000000000000000000L, 0b00000110001010000000000000000000L,
        // X^52, X^53
        0b00001100010100000000000000000000L, 0b00011000101000000000000000000000L,
        // X^54, X^55
        0b00110001010000000000000000000000L, 0b01100010100000000000000000000000L,
        // X^56, X^57
        0b11000101000000000000000000000000L, 0b10001010000000000000000011000101L,
        // X^58, X^59
        0b00010100000000000000000101001111L, 0b00101000000000000000001010011110L,
        // X^60, X^61
        0b01010000000000000000010100111100L, 0b10100000000000000000101001111000L,
        // X^62, X^63
        0b01000000000000000001010000110101L, 0b10000000000000000010100001101010L,
    };

    public JdkGf032(EnvType envType) {
        super(envType, 32);
    }

    @Override
    public Gf2eType getGf2eType() {
        return Gf2eType.JDK;
    }

    @Override
    public boolean validateElement(byte[] p) {
        return p.length == 4;
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
        // The order of GF(2^32) = 2^32. We can calculate p^{-1} as p^{2^{32}-2} so that p^{-1} * p = p^{2^{32}-1} = 1
        byte[] a = BytesUtils.clone(p);
        byte[] r = new byte[byteL];
        for (int i = 0; i <= 4; i++) {
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
        // The order of GF(2^32) = 2^32. We can calculate p^{-1} as p^{2^{32}-2} so that p^{-1} * p = p^{2^{32}-1} = 1
        byte[] a = BytesUtils.clone(p);
        for (int i = 0; i <= 4; i++) {
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
        return ((x[0] & 0b11111111L) << 24)
            | ((x[1] & 0b11111111L) << 16)
            | ((x[2] & 0b11111111L) << 8)
            | (x[3] & 0b11111111L);
    }

    private void asBytes(long x, byte[] z) {
        z[0] = (byte) (x >>> 24);
        z[1] = (byte) (x >>> 16);
        z[2] = (byte) (x >>> 8);
        z[3] = (byte) x;
    }
}
