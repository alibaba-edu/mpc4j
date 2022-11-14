/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.utils.Curve25519FieldUtils;

import java.util.Arrays;

/**
 * A field element of the field $\mathbb{Z} / (2^{255} - 19)$. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/FieldElement.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/6
 */
class CafeFieldElement {
    /**
     * field int size
     */
    static final int INT_SIZE = Curve25519FieldUtils.INT_SIZE;
    /**
     * field byte size
     */
    static final int BYTE_SIZE = Curve25519FieldUtils.BYTE_SIZE;
    /**
     * 0
     */
    static final CafeFieldElement ZERO = new CafeFieldElement(Curve25519FieldUtils.ZERO_INTS);
    /**
     * 1
     */
    static final CafeFieldElement ONE = new CafeFieldElement(Curve25519FieldUtils.ONE_INTS);
    /**
     * -1
     */
    static final CafeFieldElement MINUS_ONE = new CafeFieldElement(Curve25519FieldUtils.MINUS_ONE_INTS);

    /**
     * An element $t$, entries $t[0] \dots t[9]$, represents the integer $t[0] +
     * 2^{26} t[1] + 2^{51} t[2] + 2^{77} t[3] + 2^{102} t[4] + \dots + 2^{230}
     * t[9]$. Bounds on each $t[i]$ vary depending on context.
     */
    final int[] t;

    /**
     * Create a field element.
     *
     * @param t The $2^{25.5}$ bit representation of the field element.
     */
    CafeFieldElement(int[] t) {
        if (t.length != INT_SIZE) {
            throw new IllegalArgumentException("Invalid radix-2^25.5 representation");
        }
        this.t = t;
    }

    /**
     * Decode a FieldElement from the low 255 bits of a 256-bit input.
     *
     * @param in The 32-byte representation.
     * @return The field element in its $2^{25.5}$ bit representation.
     */
    public static CafeFieldElement decode(byte[] in) {
        if (in.length != BYTE_SIZE) {
            throw new IllegalArgumentException("Invalid byte[] representation");
        }
        int[] result = Curve25519FieldUtils.decode(in);
        return new CafeFieldElement(result);
    }

    /**
     * Encode a FieldElement in its 32-byte representation.
     * <p>
     * This is done in two steps:
     * <ol>
     * <li>Reduce the value of the field element modulo $p$.
     * <li>Convert the field element to the 32 byte representation.
     * <p>
     * The idea for the modulo $p$ reduction algorithm is as follows:
     * <h2>Assumption:</h2>
     * <ul>
     * <li>$p = 2^{255} - 19$
     * <li>$h = h_0 + 2^{25} * h_1 + 2^{(26+25)} * h_2 + \dots + 2^{230} * h_9$
     * where $0 \le |h_i| \lt 2^{27}$ for all $i=0,\dots,9$.
     * <li>$h \cong r \mod p$, i.e. $h = r + q * p$ for some suitable $0 \le r \lt
     * p$ and an integer $q$.
     * </ul>
     * <p>
     * Then $q = [2^{-255} * (h + 19 * 2^{-25} * h_9 + 1/2)]$ where $[x] =
     * floor(x)$.
     * <h2>Proof:</h2>
     * <p>
     * We begin with some very raw estimation for the bounds of some expressions:
     * <p>
     * $$ \begin{equation} |h| \lt 2^{230} * 2^{30} = 2^{260} \Rightarrow |r + q *
     * p| \lt 2^{260} \Rightarrow |q| \lt 2^{10}. \\ \Rightarrow -1/4 \le a := 19^2
     * * 2^{-255} * q \lt 1/4. \\ |h - 2^{230} * h_9| = |h_0 + \dots + 2^{204} *
     * h_8| \lt 2^{204} * 2^{30} = 2^{234}. \\ \Rightarrow -1/4 \le b := 19 *
     * 2^{-255} * (h - 2^{230} * h_9) \lt 1/4 \end{equation} $$
     * <p>
     * Therefore $0 \lt 1/2 - a - b \lt 1$.
     * <p>
     * Set $x := r + 19 * 2^{-255} * r + 1/2 - a - b$. Then:
     * <p>
     * $$ 0 \le x \lt 255 - 20 + 19 + 1 = 2^{255} \\ \Rightarrow 0 \le 2^{-255} * x
     * \lt 1. $$
     * <p>
     * Since $q$ is an integer we have
     * <p>
     * $$ [q + 2^{-255} * x] = q \quad (1) $$
     * <p>
     * Have a closer look at $x$:
     * <p>
     * $$ \begin{align} x &amp;= h - q * (2^{255} - 19) + 19 * 2^{-255} * (h - q *
     * (2^{255} - 19)) + 1/2 - 19^2 * 2^{-255} * q - 19 * 2^{-255} * (h - 2^{230} *
     * h_9) \\ &amp;= h - q * 2^{255} + 19 * q + 19 * 2^{-255} * h - 19 * q + 19^2 *
     * 2^{-255} * q + 1/2 - 19^2 * 2^{-255} * q - 19 * 2^{-255} * h + 19 * 2^{-25} *
     * h_9 \\ &amp;= h + 19 * 2^{-25} * h_9 + 1/2 - q^{255}. \end{align} $$
     * <p>
     * Inserting the expression for $x$ into $(1)$ we get the desired expression for
     * $q$.
     *
     * @return the 32-byte encoding of this FieldElement.
     */
    byte[] encode() {
        return Curve25519FieldUtils.encode(t);
    }

    /**
     * Constant-time equality check. Compares the encodings of the two FieldElements.
     *
     * @return 1 if self and other are equal, 0 otherwise.
     */
    public int cequals(CafeFieldElement other) {
        return Curve25519FieldUtils.areEqual(t, other.t);
    }

    /**
     * Constant-time selection between two FieldElements.
     * <p>
     * Implemented as a conditional copy. Logic is inspired by the SUPERCOP implementation.
     * </p>
     * This function is identical to CMOV(a, that, c):
     * <p>
     * If c is False, CMOV returns a, otherwise it returns that.
     * For constant-time implementations, this operation must run in time independent of the value of c.
     * </p>
     *
     * @param that the other field element.
     * @param c    must be 0 or 1, otherwise results are undefined.
     * @return a copy of this if $c == 0$, or a copy of that if $c == 1$.
     * @see <a href=
     * "https://github.com/floodyberry/supercop/blob/master/crypto_sign/ed25519/ref10/fe_cmov.c"
     * target="_top">SUPERCOP</a>
     */
    public CafeFieldElement cmov(CafeFieldElement that, int c) {
        int[] result = Curve25519FieldUtils.createZero();
        Curve25519FieldUtils.copy(t, result);
        Curve25519FieldUtils.cmov(c, that.t, result);
        return new CafeFieldElement(result);
    }

    /**
     * Compute the absolute value of this FieldElement in constant time.
     *
     * @return $|\text{this}|$.
     */
    CafeFieldElement abs() {
        return cmov(neg(), isNeg());
    }

    /**
     * Equality check overridden to be constant-time. Fails fast if the objects are of different types.
     *
     * @return true if self and other are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CafeFieldElement)) {
            return false;
        }

        CafeFieldElement other = (CafeFieldElement) obj;
        return cequals(other) == 1;
    }

    @Override
    public int hashCode() {
        // The general contract for the hashCode method states that equal objects must
        // have equal hash codes. Object equality is based on the encodings of the
        // field elements, not their internal representations (which may not be
        // canonical).
        final byte[] s = encode();
        return Arrays.hashCode(s);
    }

    /**
     * Determine whether this FieldElement is zero.
     *
     * @return 1 if this FieldElement is zero, 0 otherwise.
     */
    int isZero() {
        return Curve25519FieldUtils.isZero(t);
    }

    /**
     * Determine whether this FieldElement is negative.
     * <p>
     * As in RFC 8032, a FieldElement is negative if the least significant bit of the encoding is 1.
     * </p>
     *
     * @return 1 if this FieldElement is negative, 0 otherwise.
     * @see <a href="https://tools.ietf.org/html/rfc8032" target="_top">RFC 8032</a>
     */
    int isNeg() {
        return Curve25519FieldUtils.isNeg(t);
    }

    /**
     * $h = f + g$
     * <p>
     * Preconditions:
     * <ul>
     * <li>$|f|$ bounded by $1.1*2^{25},1.1*2^{24},1.1*2^{25},1.1*2^{24},$ etc.
     * <li>$|g|$ bounded by $1.1*2^{25},1.1*2^{24},1.1*2^{25},1.1*2^{24},$ etc.
     * </ul>
     * <p>
     * Postconditions:
     * <ul>
     * <li>$|h|$ bounded by $1.1*2^{26},1.1*2^{25},1.1*2^{26},1.1*2^{25},$ etc.
     * </ul>
     *
     * @param val The field element to add.
     * @return The field element this + val.
     */
    public CafeFieldElement add(CafeFieldElement val) {
        int[] h = Curve25519FieldUtils.createZero();
        Curve25519FieldUtils.add(t, val.t, h);
        return new CafeFieldElement(h);
    }

    /**
     * $h = f - g$
     * <p>
     * Can overlap $h$ with $f$ or $g$.
     * <p>
     * Preconditions:
     * <ul>
     * <li>$|f|$ bounded by $1.1*2^{25},1.1*2^{24},1.1*2^{25},1.1*2^{24},$ etc.
     * <li>$|g|$ bounded by $1.1*2^{25},1.1*2^{24},1.1*2^{25},1.1*2^{24},$ etc.
     * </ul>
     * <p>
     * Postconditions:
     * <ul>
     * <li>$|h|$ bounded by $1.1*2^{26},1.1*2^{25},1.1*2^{26},1.1*2^{25},$ etc.
     * </ul>
     *
     * @param val The field element to subtract.
     * @return The field element this - val.
     **/
    public CafeFieldElement sub(CafeFieldElement val) {
        int[] h = Curve25519FieldUtils.createZero();
        Curve25519FieldUtils.sub(t, val.t, h);
        return new CafeFieldElement(h);
    }

    /**
     * $h = -f$
     * <p>
     * Preconditions:
     * <ul>
     * <li>$|f|$ bounded by $1.1*2^{25},1.1*2^{24},1.1*2^{25},1.1*2^{24},$ etc.
     * <p>
     * Postconditions:
     * <ul>
     * <li>$|h|$ bounded by $1.1*2^{25},1.1*2^{24},1.1*2^{25},1.1*2^{24},$ etc.
     *
     * @return The field element (-1) * this.
     */
    public CafeFieldElement neg() {
        int[] h = Curve25519FieldUtils.createZero();
        Curve25519FieldUtils.neg(t, h);
        return new CafeFieldElement(h);
    }

    /**
     * $h = f * g$
     * <p>
     * Can overlap $h$ with $f$ or $g$.
     * <p>
     * Preconditions:
     * <ul>
     * <li>$|f|$ bounded by $1.65*2^{26},1.65*2^{25},1.65*2^{26},1.65*2^{25},$ etc.
     * <li>$|g|$ bounded by $1.65*2^{26},1.65*2^{25},1.65*2^{26},1.65*2^{25},$ etc.
     * <p>
     * Postconditions:
     * <ul>
     * <li>$|h|$ bounded by $1.01*2^{25},1.01*2^{24},1.01*2^{25},1.01*2^{24},$ etc.
     * <p>
     * Notes on implementation strategy:
     * <p>
     * Using schoolbook multiplication. Karatsuba would save a little in some cost
     * models.
     * <p>
     * Most multiplications by 2 and 19 are 32-bit precomputations; cheaper than
     * 64-bit postcomputations.
     * <p>
     * There is one remaining multiplication by 19 in the carry chain; one *19
     * precomputation can be merged into this, but the resulting data flow is
     * considerably less clean.
     * <p>
     * There are 12 carries below. 10 of them are 2-way parallelizable and
     * vectorizable. Can get away with 11 carries, but then data flow is much
     * deeper.
     * <p>
     * With tighter constraints on inputs can squeeze carries into int32.
     *
     * @param val The field element to multiply.
     * @return The (reasonably reduced) field element this * val.
     */
    public CafeFieldElement mul(CafeFieldElement val) {
        int[] result = Curve25519FieldUtils.createZero();
        Curve25519FieldUtils.mul(t, val.t, result);
        return new CafeFieldElement(result);
    }

    /**
     * $h = f * f$
     * <p>
     * Can overlap $h$ with $f$.
     * <p>
     * Preconditions:
     * <ul>
     * <li>$|f|$ bounded by $1.65*2^{26},1.65*2^{25},1.65*2^{26},1.65*2^{25},$ etc.
     * <p>
     * Postconditions:
     * <ul>
     * <li>$|h|$ bounded by $1.01*2^{25},1.01*2^{24},1.01*2^{25},1.01*2^{24},$ etc.
     * <p>
     * See {@link #mul(CafeFieldElement)} for discussion of implementation
     * strategy.
     *
     * @return The (reasonably reduced) square of this field element.
     */
    public CafeFieldElement sqr() {
        int[] result = Curve25519FieldUtils.createZero();
        Curve25519FieldUtils.sqr(t, result);
        return new CafeFieldElement(result);
    }

    /**
     * $h = 2 * f * f$
     * <p>
     * Can overlap $h$ with $f$.
     * <p>
     * Preconditions:
     * <ul>
     * <li>$|f|$ bounded by $1.65*2^{26},1.65*2^{25},1.65*2^{26},1.65*2^{25},$ etc.
     * </ul>
     * <p>
     * Postconditions:
     * <ul>
     * <li>$|h|$ bounded by $1.01*2^{25},1.01*2^{24},1.01*2^{25},1.01*2^{24},$ etc.
     * </ul>
     * <p>
     * See {@link #mul(CafeFieldElement)} for discussion of implementation
     * strategy.
     *
     * @return The (reasonably reduced) square of this field element times 2.
     */
    public CafeFieldElement sqrDbl() {
        int[] result = Curve25519FieldUtils.createZero();
        Curve25519FieldUtils.sqrDbl(t, result);
        return new CafeFieldElement(result);
    }

    /**
     * Invert this field element.
     * <p>
     * The inverse is found via Fermat's little theorem:<br>
     * $a^p \cong a \mod p$ and therefore $a^{(p-2)} \cong a^{-1} \mod p$
     * </p>
     *
     * @return The inverse of this field element.
     */
    public CafeFieldElement inv() {
        int[] result = Curve25519FieldUtils.createZero();
        Curve25519FieldUtils.inv(t, result);
        return new CafeFieldElement(result);
    }

    /**
     * Raises this field element to the power $(p - 5) / 8 = 2^{252} - 3$.
     * <p>
     * Helper for {@link #sqrtRatioM1(CafeFieldElement, CafeFieldElement)}.
     * </p>
     *
     * @return $\text{this}^{(p-5)/8}$.
     */
    CafeFieldElement powPm5d8() {
        int[] result = Curve25519FieldUtils.createZero();
        Curve25519FieldUtils.powPm5d8(t, result);
        return new CafeFieldElement(result);
    }

    /**
     * The result of calling {@link #sqrtRatioM1(CafeFieldElement, CafeFieldElement)}.
     */
    public static class SqrtRatioM1Result {
        /**
         * <ul>
         * <li>true (1) if $v$ is non-zero and $u / v$ is square.
         * <li>true (1) if $u$ is zero.
         * <li>false (0) if $v$ is zero and $u$ is non-zero.
         * <li>false (0) if $u / v$ is non-square (so $i * u / v$ is square).
         * </ul>
         */
        final int wasSquare;
        /**
         * <ul>
         * <li>+$\sqrt{u / v}$ if $v$ is non-zero and $u / v$ is square.
         * <li>zero if $u$ is zero.
         * <li>zero if $v$ is zero and $u$ is non-zero.
         * <li>+$\sqrt{i * u / v}$ if $u / v$ is non-square (so $i * u / v$ is square).
         * </ul>
         */
        final CafeFieldElement result;

        SqrtRatioM1Result(int wasSquare, CafeFieldElement result) {
            this.wasSquare = wasSquare;
            this.result = result;
        }
    }

    /**
     * Given FieldElements $u$ and $v$, compute either $\sqrt{u / v}$ or $\sqrt{i * u / v}$ in constant time.
     * <p>
     * This function always returns the non-negative square root.
     * </p>
     *
     * @param u the numerator.
     * @param v the denominator.
     * @return <ul>
     * <li>(true, +$\sqrt{u / v}$) if $v$ is non-zero and $u / v$ is square.
     * <li>(true, zero) if $u$ is zero.
     * <li>(false, zero) if $v$ is zero and $u$ is non-zero.
     * <li>(false, +$\sqrt{i * u / v}$) if $u / v$ is non-square (so $i * u / v$ is square).
     * </ul>
     */
    static SqrtRatioM1Result sqrtRatioM1(CafeFieldElement u, CafeFieldElement v) {
        CafeFieldElement v3 = v.sqr().mul(v);
        CafeFieldElement v7 = v3.sqr().mul(v);
        CafeFieldElement r = u.mul(v3).mul(u.mul(v7).powPm5d8());
        CafeFieldElement check = v.mul(r.sqr());

        CafeFieldElement uNeg = u.neg();
        int correctSignSqrt = check.cequals(u);
        int flippedSignSqrt = check.cequals(uNeg);
        int flippedSignSqrtM1 = check.cequals(uNeg.mul(CafeConstants.SQRT_M1));

        CafeFieldElement rPrime = r.mul(CafeConstants.SQRT_M1);
        r = r.cmov(rPrime, flippedSignSqrt | flippedSignSqrtM1);

        // Choose the non-negative square root.
        r = r.abs();

        return new SqrtRatioM1Result(correctSignSqrt | flippedSignSqrt, r);
    }

    @Override
    public String toString() {
        StringBuilder ir = new StringBuilder("FieldElement([");
        for (int i = 0; i < t.length; i++) {
            if (i > 0) {
                ir.append(", ");
            }
            ir.append(t[i]);
        }
        ir.append("])");
        return ir.toString();
    }
}
