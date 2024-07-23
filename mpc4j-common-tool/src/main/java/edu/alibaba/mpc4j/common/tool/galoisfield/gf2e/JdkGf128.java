package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.bouncycastle.util.Longs;
import org.bouncycastle.util.Pack;

/**
 * GF(2^128) using pure Java. The implementation comes from "org.bouncycastle.crypto.modes.gcm.GCMUtil.java". Here we
 * modify codes to reduce copying operations.
 * <p></p>
 * The blog "https://blog.quarkslab.com/reversing-a-finite-field-multiplication-optimization.html" clearly describe the
 * detailed algorithm.
 * <p></p>
 * In 2009, in [GK2010], Gueron and Kounavis proposed an optimization of the reduction modulo $𝑃(𝑋) which only uses
 * shifts and XORs and was taking place after the reflection of the inputs and their multiplication.
 * <p></p>
 * Their technique relied on two steps. First they applied a reduction algorithm known as the Barret's algorithm (see
 * [BZ2010] 2.4.1) which involves 2 carry-less multiplications. The algorithm, adapted to the case of binary polynomials
 * of degree less than 256 to be reduced by a polynomial of degree 128, is given below.
 * <p></p>
 * Given X^128 (the Barrett's basis), U (the input polynomial to reduce of degree at most 255), P (the reduction
 * polynomial of degree 128), and P' (P' = X^256 div P where 'div' is the polynomial division), returns T = U mod P as
 * follows:
 * <li>Q = ((U div X^128) * P') div X^128</li>
 * <li>T = U + Q * P</li>
 * <p></p>
 * Then they took advantage of the special structure of the reduction polynomial to devise an optimization only relying
 * on shifts and XORs. Indeed, going into details, for P(X) = X^128 + X^7 + X^2 + X + 1, we have P' = X^256 div P = P.
 * Then Line 2 of the function (computation of the quotient Q) can be rewritten as:
 * <p>Input: 256-bit operand [X3 : X2 : X1 : X0]</p>
 * <li>A = X3 >> 63</li>
 * <li>B = X3 >> 62</li>
 * <li>C = X3 >> 57</li>
 * <li>D = X2 ⨁ A ⨁ B ⨁ C</li>
 * <p>Output: D</p>
 * Line 3 of the algorithm is in fact equivalent to T = (U mod X^128) + (Q * (P + X^128)) as the remainder has degree
 * less than the modulus. This means we only consider the 128 least significant coefficients of U and we multiply Q by
 * X^7 + X^2 + X + 1. This gives the following optimization:
 * <p>Input: 256-bit operand [X3 : X2 : X1 : X0] and D computed beforehand</p>
 * <li>[E1 : E0] = [X3 : D] << 1</li>
 * <li>[F1 : F0] = [X3 : D] << 2</li>
 * <li>[G1 : G0] = [X3 : D] << 7</li>
 * <p>Output: [X3 ⨁ E1 ⨁ F1 ⨁ G1 ⨁ X1 : D ⨁ E0 ⨁ F0 ⨁ G0 ⨁ X0]</p>
 *
 * @author Weiran Liu
 * @date 2024/6/4
 */
public class JdkGf128 extends AbstractGf128 {

    public JdkGf128(EnvType envType) {
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
        long[] x = asLongs(p);
        long[] y = asLongs(q);
        // GF(2^κ) multiplication is done in two steps: (1) carry-less multiplication; (2) module reduction.
        // 1. carry-less multiplication: we use ``one iteration carry-less Karatsuba'' carry-less multiplication.
        // we use rev(x) * rev(y) == rev((x * y) << 1) to compute the high part of a 64x64 product x * y,
        // where rev(·) is the bit reversal of the input.
        long x0 = x[0], x1 = x[1];
        long y0 = y[0], y1 = y[1];
        long x0r = Longs.reverse(x0), x1r = Longs.reverse(x1);
        long y0r = Longs.reverse(y0), y1r = Longs.reverse(y1);
        // Step 1: multiply carry-less the following operands: A1 with B1, A0 with B0, and A0 ⊕ A1 with B0 ⊕ B1.
        // Let the results of the above three multiplications be: [C1 : C0], [D1 : D0] and [E1 : E0], respectively.
        // [C1 : C0] = A1 ● B1
        long h3 = Long.reverse(JdkGf2eUtils.implMul64(x1r, y1r) << 1);
        long h2 = JdkGf2eUtils.implMul64(x1, y1);
        // [D1 : D0] = A0 ● B0
        long h1 = Long.reverse(JdkGf2eUtils.implMul64(x0r, y0r) << 1);
        long h0 = JdkGf2eUtils.implMul64(x0, y0);
        // [E1 : E0] = (A0 ⊕ A1) ● (B0 ⊕ B1)
        long h5 = Long.reverse(JdkGf2eUtils.implMul64(x0r ^ x1r, y0r ^ y1r) << 1);
        long h4 = JdkGf2eUtils.implMul64(x0 ^ x1, y0 ^ y1);
        // Step 2: construct the 256-bit output of the multiplication [A1:A0] ● [B1:B0] as follows:
        // [A1 : A0] ● [B1 : B0] = [C1 : C0 ⊕ C1 ⊕ D1 ⊕ E1 : D1 ⊕ C0 ⊕ D0 ⊕ E0 : D0]
        //noinspection UnnecessaryLocalVariable
        long z3  = h3;
        long z2  = h2 ^ h3 ^ h1 ^ h5;
        long z1  = h1 ^ h2 ^ h0 ^ h4;
        //noinspection UnnecessaryLocalVariable
        long z0  = h0;
        // 2. reduction modulo P(X) = X^128 + X^7 + X^2 + X + 1
        // Denote the input operand by [X3 : X2 : X1 : X0] where X3, X2, X1 and X0 are 64 bit long each.
        // Step 1: shift X3 by 63, 62 and 57-bit positions to the right, i.e., compute:
        //  A = X3 >> 63, B = X3 >> 62, C = X3 >> 57
        long a = z3 >>> 63, b = z3 >>> 62, c = z3 >>> 57;
        // Step 2: We XOR A, B, and C with X2, i.e., compute a number D as follows: D = A ⊕ B ⊕ C ⊕ X2
        long d = z2 ^ a ^ b ^ c;
        // Step 3: shift [X3 : D] by 1, 2 and 7 bit positions to the left, i.e., compute the following numbers:
        // [E1 : E0] = [X3 : D] << 1, [F1 : F0] = [X3 : D] << 2, [G1 : G0] = [X3 : D] << 7
        long e0 = d << 1;
        long e1 = (z3 << 1) ^ (d >>> 63);
        long f0 = d << 2;
        long f1 = (z3 << 2) ^ (d >>> 62);
        long g0 = d << 7;
        long g1 = (z3 << 7) ^ (d >>> 57);
        // Step 4: XOR [E1 : E0], [F1 : F0], and [G1 : G0] with each other and [X3 : D], i.e., compute the following:
        // [H1 : H0] = [X3 ⊕ E1 ⊕ F1 ⊕ G1 : D ⊕ E0 ⊕ F0 ⊕ G0]
        long i1 = z3 ^ e1 ^ f1 ^ g1;
        long i0 = d ^ e0 ^ f0 ^ g0;
        // Return [X1 ⊕ H1 : X0 ⊕ H0]
        x[1] = z1 ^ i1;
        x[0] = z0 ^ i0;
        asBytes(x, p);
    }

    @Override
    public byte[] inv(byte[] p) {
        assert validateNonZeroElement(p);
        // The order of GF(2^128) = 2^128. We can calculate p^{-1} as p^{2^{128}-2} so that p^{-1} * p = p^{2^{128}-1} = 1
        // The addition chain below requires 142 mul/sqr operations total.
        byte[] a = BytesUtils.clone(p);
        byte[] r = new byte[byteL];
        for (int i = 0; i <= 6; i++) {
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
        System.arraycopy(y, 0, p, 0, byteL);
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

    private long[] asLongs(byte[] x) {
        long[] z = new long[2];
        Pack.bigEndianToLong(x, 0, z, 1, 1);
        Pack.bigEndianToLong(x, 8, z, 0, 1);
        return z;
    }

    private void asBytes(long[] x, byte[] z) {
        Pack.longToBigEndian(x, 1, 1, z, 0);
        Pack.longToBigEndian(x, 0, 1, z, 8);
    }
}
