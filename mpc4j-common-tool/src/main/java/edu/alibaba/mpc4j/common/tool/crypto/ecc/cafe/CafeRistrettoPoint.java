/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import java.util.Arrays;

/**
 * An element of the prime-order ristretto255 group. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/RistrettoElement.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/11
 */
public class CafeRistrettoPoint {
    /**
     * identity
     */
    public static final CafeRistrettoPoint IDENTITY = new CafeRistrettoPoint(CafeEdwardsPoint.IDENTITY);

    /**
     * The internal representation. Not canonical.
     */
    final CafeEdwardsPoint repr;

    /**
     * Only for internal use.
     */
    CafeRistrettoPoint(CafeEdwardsPoint repr) {
        this.repr = repr;
    }

    /**
     * The function MAP(r0) from section 3.2.4 of the ristretto255 ID.
     * <p>
     * See https://ristretto.group/details/elligator_in_extended.html Section Elligator for ristretto 255 in extended
     * coordinates for more details.
     * </p>
     *
     * @param r0 a field element.
     * @return encoded Ristretto element.
     */
    static CafeRistrettoPoint map(final CafeFieldElement r0) {
        // r = √(-1) · r_0^2
        final CafeFieldElement r = r0.sqr().mul(CafeConstants.SQRT_M1);
        // N_s = (r + 1) · (1 - d^2)
        final CafeFieldElement ns = r.add(CafeFieldElement.ONE).mul(CafeConstants.ONE_MINUS_D_SQ);
        // c = -1, D = (c - dr) · (r + d)
        final CafeFieldElement d = CafeFieldElement.MINUS_ONE.sub(r.mul(CafeConstants.EDWARDS_D))
            .mul(r.add(CafeConstants.EDWARDS_D));

        // s = sqrt_ratio_i(N_s, D)
        final CafeFieldElement.SqrtRatioM1Result sqrt = CafeFieldElement.sqrtRatioM1(ns, d);
        CafeFieldElement s = sqrt.result;

        // s' = -|s · r_0|
        final CafeFieldElement sPrime = s.mul(r0).abs().neg();
        // if sqrt, s = s'
        s = sPrime.cmov(s, sqrt.wasSquare);
        // if sqrt, c = r
        final CafeFieldElement c = r.cmov(CafeFieldElement.MINUS_ONE, sqrt.wasSquare);

        // N_t = c · (r - 1) · (d - 1)^2 - d
        final CafeFieldElement nt = c.mul(r.sub(CafeFieldElement.ONE)).mul(CafeConstants.D_MINUS_ONE_SQ).sub(d);
        // s^2
        final CafeFieldElement sSq = s.sqr();

        // W_0 = 2 · s · D
        final CafeFieldElement w0 = s.add(s).mul(d);
        // W_1 = N_t · √(a · d - 1)
        final CafeFieldElement w1 = nt.mul(CafeConstants.SQRT_AD_MINUS_ONE);
        // W_2 = 1 - s^2
        final CafeFieldElement w2 = CafeFieldElement.ONE.sub(sSq);
        // W_3 = 1 + s^2
        final CafeFieldElement w3 = CafeFieldElement.ONE.add(sSq);

        // Return (W_0 · W_3, W_2 · W_1, W_1 · W_3, W_0 · W_2)
        return new CafeRistrettoPoint(new CafeEdwardsPoint(w0.mul(w3), w2.mul(w1), w1.mul(w3), w0.mul(w2)));
    }

    /**
     * Construct a ristretto255 element from a uniformly-distributed 64-byte string. This is the ristretto255
     * FROM_UNIFORM_BYTES function.
     * <p>
     * See https://ristretto.group/formulas/elligator.html, Section Hash-to-ristretto255 for details.
     * </p>
     *
     * @param b a uniformly-distributed 64-byte string.
     * @return the resulting element.
     */
    public static CafeRistrettoPoint fromUniformBytes(final byte[] b) {
        // r0 is the low 255 bites of bytes[0..32], taken mod p.
        final byte[] b0 = Arrays.copyOfRange(b, 0, CafeFieldElement.BYTE_SIZE);
        final CafeFieldElement r0 = CafeFieldElement.decode(b0);

        // r1 is the low 255 bites of bytes[32..64], taken mod p.
        final byte[] b1 = Arrays.copyOfRange(b, CafeFieldElement.BYTE_SIZE, CafeFieldElement.BYTE_SIZE * 2);
        final CafeFieldElement r1 = CafeFieldElement.decode(b1);

        // Apply the Elligator map below to the inputs r_0, r_1 to obtain points P_1, P_2
        final CafeRistrettoPoint point1 = CafeRistrettoPoint.map(r0);
        final CafeRistrettoPoint point2 = CafeRistrettoPoint.map(r1);

        // and returns P_1 + P_2
        return point1.add(point2);
    }

    /**
     * Compress this element using the Ristretto encoding. This is the ristretto255 ENCODE function.
     * <p>
     * See https://ristretto.group/formulas/encoding.html, Section Encoding from Extended Coordinates for details.
     * </p>
     *
     * @return the encoded element.
     */
    public CafeRistrettoCompressedPoint compress() {
        // u_1 = (z + y) · (z - y)
        final CafeFieldElement u1 = repr.z.add(repr.y).mul(repr.z.sub(repr.y));
        // u_2 = x · y
        final CafeFieldElement u2 = repr.x.mul(repr.y);

        // I = invsqrt(u_1 · u_2^2). The inverse square root always exists when (x, y, z, t) is a valid representative.
        final CafeFieldElement.SqrtRatioM1Result invsqrt
            = CafeFieldElement.sqrtRatioM1(CafeFieldElement.ONE, u1.mul(u2.sqr()));

        // d_1 = u_i · I
        final CafeFieldElement d1 = invsqrt.result.mul(u1);
        // d_2 = u_2 · I
        final CafeFieldElement d2 = invsqrt.result.mul(u2);
        // z_inv = d_1 · d_2 · t
        final CafeFieldElement zInv = d1.mul(d2).mul(repr.t);

        // x · √a
        final CafeFieldElement ix = repr.x.mul(CafeConstants.SQRT_M1);
        // y · √a
        final CafeFieldElement iy = repr.y.mul(CafeConstants.SQRT_M1);
        // d_1 / (√(a - d))
        final CafeFieldElement den1 = d1.mul(CafeConstants.INVSQRT_A_MINUS_D);
        // if t · z_inv is negative, (x, y) = (-y · √a, x · √a)
        final int rotate = repr.t.mul(zInv).isNeg();
        final CafeFieldElement x = repr.x.cmov(iy, rotate);
        CafeFieldElement y = repr.y.cmov(ix, rotate);
        final CafeFieldElement z = repr.z;
        // if t · z_inv is negative, d_1 / (√(a - d)), else d = d_2
        final CafeFieldElement d = d2.cmov(den1, rotate);
        // if x · z_inv is negative, set y = -y
        y = y.cmov(y.neg(), x.mul(zInv).isNeg());

        // compute s = √(-a) · (z - y) · d.
        // Since a = ±1, implementations can replace multiplications by a with sign changes, as appropriate.
        CafeFieldElement s = d.mul(z.sub(y));
        final int sIsNegative = s.isNeg();
        s = s.cmov(s.neg(), sIsNegative);

        // Return the canonical little-endian encoding of s.
        return new CafeRistrettoCompressedPoint(s.encode());
    }

    /**
     * Constant-time equality check. This is the ristretto255 EQUALS function.
     *
     * @param other the other Ristretto element.
     * @return 1 if this and other are equal, 0 otherwise.
     */
    public int cequals(final CafeRistrettoPoint other) {
        CafeFieldElement x1y2 = repr.x.mul(other.repr.y);
        CafeFieldElement y1x2 = repr.y.mul(other.repr.x);
        CafeFieldElement y1y2 = repr.y.mul(other.repr.y);
        CafeFieldElement x1x2 = repr.x.mul(other.repr.x);
        return x1y2.cequals(y1x2) | y1y2.cequals(x1x2);
    }

    /**
     * Constant-time selection between two RistrettoElements.
     *
     * @param that the other element.
     * @param c    must be 0 or 1, otherwise results are undefined.
     * @return a copy of this if $c == 0$, or a copy of that if $c == 1$.
     */
    public CafeRistrettoPoint cmov(final CafeRistrettoPoint that, final int c) {
        return new CafeRistrettoPoint(this.repr.cmove(that.repr, c));
    }

    /**
     * Equality check overridden to be constant-time. Fails fast if the objects are of different types.
     *
     * @return true if this and other are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CafeRistrettoPoint)) {
            return false;
        }

        CafeRistrettoPoint other = (CafeRistrettoPoint) obj;
        return cequals(other) == 1;
    }

    @Override
    public int hashCode() {
        // The general contract for the hashCode method states that equal objects must
        // have equal hash codes. Object equality is based on the encodings of the
        // elements, not their internal representations (which are not canonical). Note
        // that equality is actually implemented using the ristretto255 EQUALS function,
        // but it is simpler to derive a hashCode from the element's encoding.
        return compress().hashCode();
    }

    /**
     * Group addition.
     *
     * @param q the element to add to this one.
     * @return $P + Q$.
     */
    public CafeRistrettoPoint add(final CafeRistrettoPoint q) {
        return new CafeRistrettoPoint(repr.add(q.repr));
    }

    /**
     * Group subtraction.
     *
     * @param q the element to subtract from this one.
     * @return $P - Q$.
     */
    public CafeRistrettoPoint sub(final CafeRistrettoPoint q) {
        return new CafeRistrettoPoint(repr.sub(q.repr));
    }

    /**
     * Element negation.
     *
     * @return $-P$.
     */
    public CafeRistrettoPoint neg() {
        return new CafeRistrettoPoint(repr.neg());
    }

    /**
     * Element doubling.
     *
     * @return $[2]P$
     */
    public CafeRistrettoPoint dbl() {
        return new CafeRistrettoPoint(repr.dbl());
    }

    /**
     * Constant-time variable-base scalar multiplication.
     *
     * @param s the Scalar to multiply by.
     * @return $[s]P$.
     */
    public CafeRistrettoPoint mul(final CafeScalar s) {
        return new CafeRistrettoPoint(repr.mul(s));
    }

    @Override
    public String toString() {
        return "RistrettoElement(" + repr.toString() + ")";
    }
}
