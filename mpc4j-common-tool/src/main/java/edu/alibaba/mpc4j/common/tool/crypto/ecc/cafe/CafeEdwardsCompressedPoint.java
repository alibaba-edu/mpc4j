/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import java.util.Arrays;

/**
 * An Edwards point encoded in "Edwards y" / "Ed25519" format. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/CompressedEdwardsY.java
 * </p>
 * <p>
 * In "Edwards y" / "Ed25519" format, the curve point $(x, y)$ is determined by the $y$-coordinate and the sign of $x$.
 * </p>
 * <p>
 * The first 255 bits of a CompressedEdwardsY represent the $y$-coordinate. The high bit of the 32nd byte represents
 * the sign of $x$.
 * </p>
 *
 * @author Weiran Liu
 * @date 2011/11/7
 */
public class CafeEdwardsCompressedPoint {
    /**
     * The byte size in compressed form
     */
    static final int BYTE_SIZE = 32;
    /**
     * The encoded point.
     */
    private final byte[] data;

    public CafeEdwardsCompressedPoint(byte[] data) {
        if (data.length != BYTE_SIZE) {
            throw new IllegalArgumentException("Invalid CompressedEdwardsY encoding");
        }
        this.data = data;
    }

    /**
     * Attempts to decompress to an EdwardsPoint.
     *
     * @return an EdwardsPoint, if this is a valid encoding.
     */
    public CafeEdwardsPoint decompress() {
        CafeFieldElement y = CafeFieldElement.decode(data);
        CafeFieldElement yy = y.sqr();

        // u = y^2-1
        CafeFieldElement u = yy.sub(CafeFieldElement.ONE);

        // v = d * y^2+1
        CafeFieldElement v = yy.mul(CafeConstants.EDWARDS_D).add(CafeFieldElement.ONE);

        CafeFieldElement.SqrtRatioM1Result sqrt = CafeFieldElement.sqrtRatioM1(u, v);
        if (sqrt.wasSquare != 1) {
            throw new IllegalStateException("not a valid EdwardsPoint");
        }

        CafeFieldElement x = sqrt.result.neg().cmov(
            sqrt.result, CafeConstantTimeUtils.equal(sqrt.result.isNeg(), CafeConstantTimeUtils.bit(data, 255))
        );

        return new CafeEdwardsPoint(x, y, CafeFieldElement.ONE, x.mul(y));
    }

    /**
     * Encode the point to its compressed 32-byte form.
     *
     * @return the encoded point.
     */
    public byte[] encode() {
        return data;
    }

    /**
     * Constant-time equality check.
     *
     * @return 1 if this and other are equal, 0 otherwise.
     */
    public int cequals(CafeEdwardsCompressedPoint other) {
        return CafeConstantTimeUtils.equal(data, other.data);
    }

    /**
     * Equality check overridden to be constant-time. Fails fast if the objects are of different types.
     *
     * @return true if this and other are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CafeEdwardsCompressedPoint)) {
            return false;
        }

        CafeEdwardsCompressedPoint other = (CafeEdwardsCompressedPoint) obj;
        return cequals(other) == 1;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
