/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import java.util.Arrays;

/**
 * A Ristretto element in compressed wire format. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/CompressedRistretto.java
 * </p>
 * The Ristretto encoding is canonical, so two elements are equal if and only if their encodings are equal.
 *
 * @author Weiran Liu
 * @date 2022/11/11
 */
public class CafeRistrettoCompressedPoint {
    /**
     * The byte size in compressed form
     */
    public static final int BYTE_SIZE = 32;
    /**
     * The encoded element.
     */
    private final byte[] data;

    public CafeRistrettoCompressedPoint(byte[] data) {
        if (data.length != BYTE_SIZE) {
            throw new IllegalArgumentException("Invalid CompressedRistretto encoding");
        }
        this.data = data;
    }

    /**
     * Attempts to decompress to a RistrettoElement. This is the ristretto255 DECODE function.
     * <p>
     * See https://ristretto.group/formulas/decoding.html, Section Decoding to Extended Coordinates for more details.
     * </p>
     *
     * @return a RistrettoElement, if this is the canonical encoding of an element of the ristretto255 group.
     * @throws IllegalArgumentException if this is not the canonical encoding of an element of the ristretto255 group.
     */
    public CafeRistrettoPoint decompress() {
        // decode s_bytes, a byte-string, into s, a field element.
        final CafeFieldElement s = CafeFieldElement.decode(data);
        final byte[] sBytes = s.encode();
        // check that s_bytes is the canonical encoding of a field element, or else abort.
        final int sIsCanonical = CafeConstantTimeUtils.equal(data, sBytes);
        // check that s is non-negative, or else abort.
        if (sIsCanonical == 0 || s.isNeg() == 1) {
            throw new IllegalArgumentException("Invalid ristretto255 encoding");
        }
        // s^2
        final CafeFieldElement ss = s.sqr();
        // u1 = 1 - s^2
        final CafeFieldElement u1 = CafeFieldElement.ONE.sub(ss);
        // u2 = 1 + s^2
        final CafeFieldElement u2 = CafeFieldElement.ONE.add(ss);
        // u_2^2
        final CafeFieldElement u2Sqr = u2.sqr();
        // v = a · d · u_1^2 - u_2^2
        // since a = ±1, implementations can replace multiplications by a with sign changes, as appropriate.
        final CafeFieldElement v = CafeConstants.NEG_EDWARDS_D.mul(u1.sqr()).sub(u2Sqr);
        // I = inv_sqrt(v · u_2^2)
        final CafeFieldElement.SqrtRatioM1Result invsqrt = CafeFieldElement.sqrtRatioM1(CafeFieldElement.ONE, v.mul(u2Sqr));
        // D_x = I · u_2
        final CafeFieldElement dx = invsqrt.result.mul(u2);
        // D_y = I · d_x · v
        final CafeFieldElement dy = invsqrt.result.mul(dx).mul(v);
        // x = |2 · s · D_x|, i.e., compute 2 · s · D_x and negate it if it is negative.
        final CafeFieldElement x = s.add(s).mul(dx).abs();
        // y = u_1 · D_y
        final CafeFieldElement y = u1.mul(dy);
        // t = x · y
        final CafeFieldElement t = x.mul(y);
        if (invsqrt.wasSquare == 0 || t.isNeg() == 1 || y.isZero() == 1) {
            // or abort if the square root does not exist; if t is negative or y = 0, abort.
            throw new IllegalArgumentException("Invalid ristretto255 encoding");
        } else {
            // Otherwise, return the Ristretto point represented by (x, y, 1, t).
            return new CafeRistrettoPoint(new CafeEdwardsPoint(x, y, CafeFieldElement.ONE, t));
        }
    }

    /**
     * Encode the element to its compressed 32-byte form.
     *
     * @return the encoded element.
     */
    public byte[] encode() {
        return data;
    }

    /**
     * Constant-time equality check.
     *
     * @return 1 if this and other are equal, 0 otherwise.
     */
    public int cequals(CafeRistrettoCompressedPoint other) {
        return CafeConstantTimeUtils.equal(data, other.data);
    }

    /**
     * Equality check overridden to be constant-time. Fails fast if the objects are of different types.
     *
     * @return true if this and other are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CafeRistrettoCompressedPoint)) {
            return false;
        }

        CafeRistrettoCompressedPoint other = (CafeRistrettoCompressedPoint) obj;
        return cequals(other) == 1;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
