package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.bouncycastle.util.Longs;
import org.bouncycastle.util.Pack;

/**
 * GF(2^64) using pure Java. The idea is very similar to the method used in implementing GF128.
 * <p></p>
 * The blog "https://blog.quarkslab.com/reversing-a-finite-field-multiplication-optimization.html" clearly describe the
 * detailed algorithm.
 * <p></p>
 * We just use the method shown in the Section "A Former Optimization of the Modular Reduction", but not in the Section
 * "An optimization that Needs Explanations".
 *
 * @author Weiran Liu
 * @date 2023/8/28
 */
class JdkGf064 extends AbstractGf064 {

    public JdkGf064(EnvType envType) {
        super(envType);
    }

    @Override
    public Gf2eType getGf2eType() {
        return Gf2eType.JDK;
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
        long z1 = Long.reverse(JdkGf2eUtils.implMul64(xr, yr) << 1);
        long z0 = JdkGf2eUtils.implMul64(x, y);
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
        byte[] a = BytesUtils.clone(p);
        byte[] r = new byte[byteL];
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
        assert validateNonZeroElement(p);
        // The order of GF(2^64) = 2^64. We can calculate p^{-1} as p^{2^{64}-2} so that p^{-1} * p = p^{2^{64}-1} = 1
        byte[] a = BytesUtils.clone(p);
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
        return Pack.bigEndianToLong(x, 0);
    }

    private void asBytes(long x, byte[] z) {
        Pack.longToBigEndian(x, z, 0);
    }
}
