/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

/**
 * An EdwardsPoint represents a point on the Edwards form of Curve25519. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/EdwardsPoint.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/7
 */
public class CafeEdwardsPoint {
    /**
     * Edwards曲线无穷远点
     */
    public static final CafeEdwardsPoint IDENTITY = new CafeEdwardsPoint(
        CafeFieldElement.ZERO, CafeFieldElement.ONE, CafeFieldElement.ONE, CafeFieldElement.ZERO
    );
    /**
     * x coordinate
     */
    final CafeFieldElement x;
    /**
     * y coordinate
     */
    final CafeFieldElement y;
    /**
     * z coordinate
     */
    final CafeFieldElement z;
    /**
     * t coordinate
     */
    final CafeFieldElement t;

    /**
     * Create an Edwards Point. Only for internal use.
     *
     * @param x x coordinate.
     * @param y y coordinate.
     * @param z z coordinate.
     * @param t t coordinate.
     */
    CafeEdwardsPoint(CafeFieldElement x, CafeFieldElement y, CafeFieldElement z, CafeFieldElement t) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.t = t;
    }

    /**
     * Compress this point to CompressedEdwardsY format.
     *
     * @return the encoded point.
     */
    public CafeEdwardsCompressedPoint compress() {
        CafeFieldElement recip = z.inv();
        // x = x * (1 / z)
        CafeFieldElement x = this.x.mul(recip);
        // y = y * (1 / z)
        CafeFieldElement y = this.y.mul(recip);
        byte[] s = y.encode();
        s[CafeEdwardsCompressedPoint.BYTE_SIZE - 1] |= (x.isNeg() << 7);
        return new CafeEdwardsCompressedPoint(s);
    }

    /**
     * Constant-time equality check. Compares the encodings of the two EdwardsPoints.
     *
     * @return 1 if this and other are equal, 0 otherwise.
     */
    public int cequals(CafeEdwardsPoint other) {
        return compress().cequals(other.compress());
    }

    /**
     * Constant-time selection between two EdwardsPoints.
     *
     * @param that the other point.
     * @param c    must be 0 or 1, otherwise results are undefined.
     * @return a copy of this if $c == 0$, or a copy of that if $c == 1$.
     */
    public CafeEdwardsPoint cmove(CafeEdwardsPoint that, int c) {
        return new CafeEdwardsPoint(x.cmov(that.x, c), y.cmov(that.y, c), z.cmov(that.z, c), t.cmov(that.t, c));
    }

    /**
     * Equality check overridden to be constant-time. Fails fast if the objects are of different types.
     *
     * @return true if this and other are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CafeEdwardsPoint)) {
            return false;
        }

        CafeEdwardsPoint other = (CafeEdwardsPoint) obj;
        return cequals(other) == 1;
    }

    @Override
    public int hashCode() {
        // The general contract for the hashCode method states that equal objects must
        // have equal hash codes. Object equality is based on the encodings of the
        // points, not their internal representations (which may not be canonical).
        return compress().hashCode();
    }

    /**
     * Convert the representation of this point from extended coordinates to projective coordinates.
     *
     * @return projective coordinates.
     */
    CafeProjectivePoint toProjective() {
        return new CafeProjectivePoint(x, y, z);
    }

    /**
     * Convert to a ProjectiveNielsPoint.
     *
     * @return projective Niels point.
     */
    CafeProjectiveNielsPoint toProjectiveNiels() {
        // (y + x, y - x, z, 2d * x * y)
        return new CafeProjectiveNielsPoint(y.add(x), y.sub(x), z, t.mul(CafeConstants.EDWARDS_2D));
    }

    /**
     * Dehomogenize to an AffineNielsPoint.
     *
     * @return Affiline Niels point.
     */
    CafeAffineNielsPoint toAffineNiels() {
        CafeFieldElement recip = z.inv();
        // x = x * (1 / z)
        CafeFieldElement x = this.x.mul(recip);
        // y = y * (1 / z)
        CafeFieldElement y = this.y.mul(recip);
        // 2d * x * y
        CafeFieldElement xy2d = x.mul(y).mul(CafeConstants.EDWARDS_2D);
        // y + x, y - x, 2d * x * y
        return new CafeAffineNielsPoint(y.add(x), y.sub(x), xy2d);
    }

    /**
     * Point addition.
     *
     * @param q the point to add to this one.
     * @return $P + Q$.
     */
    public CafeEdwardsPoint add(CafeEdwardsPoint q) {
        return add(q.toProjectiveNiels()).toExtended();
    }

    /**
     * Point addition.
     *
     * @param q the point to add to this one, in projective "Niels coordinates".
     * @return $P + Q$.
     */
    CafeCompletedPoint add(CafeProjectiveNielsPoint q) {
        CafeFieldElement yAddX = y.add(x);
        CafeFieldElement ySubX = y.sub(x);
        CafeFieldElement pp = yAddX.mul(q.yAddX);
        CafeFieldElement mm = ySubX.mul(q.ySubX);
        CafeFieldElement tt2d = t.mul(q.t2d);
        CafeFieldElement zz = z.mul(q.z);
        CafeFieldElement zz2 = zz.add(zz);
        return new CafeCompletedPoint(pp.sub(mm), pp.add(mm), zz2.add(tt2d), zz2.sub(tt2d));
    }

    /**
     * Point addition.
     *
     * @param q the point to add to this one, in affine "Niels coordinates".
     * @return $P + Q$.
     */
    CafeCompletedPoint add(CafeAffineNielsPoint q) {
        CafeFieldElement YPlusX = y.add(x);
        CafeFieldElement YMinusX = y.sub(x);
        CafeFieldElement PP = YPlusX.mul(q.yAddX);
        CafeFieldElement MM = YMinusX.mul(q.ySubX);
        CafeFieldElement Txy2D = t.mul(q.xy2d);
        CafeFieldElement Z2 = z.add(z);
        return new CafeCompletedPoint(PP.sub(MM), PP.add(MM), Z2.add(Txy2D), Z2.sub(Txy2D));
    }

    /**
     * Point subtraction.
     *
     * @param q the point to subtract from this one.
     * @return $P - Q$.
     */
    public CafeEdwardsPoint sub(CafeEdwardsPoint q) {
        return sub(q.toProjectiveNiels()).toExtended();
    }

    /**
     * Point subtraction.
     *
     * @param q the point to subtract from this one, in projective "Niels coordinates".
     * @return $P - Q$.
     */
    CafeCompletedPoint sub(CafeProjectiveNielsPoint q) {
        CafeFieldElement yAddX = y.add(x);
        CafeFieldElement ySubX = y.sub(x);
        CafeFieldElement pm = yAddX.mul(q.ySubX);
        CafeFieldElement mp = ySubX.mul(q.yAddX);
        CafeFieldElement tt2d = t.mul(q.t2d);
        CafeFieldElement zz = z.mul(q.z);
        CafeFieldElement zz2 = zz.add(zz);
        return new CafeCompletedPoint(pm.sub(mp), pm.add(mp), zz2.sub(tt2d), zz2.add(tt2d));
    }

    /**
     * Point subtraction.
     *
     * @param q the point to subtract from this one, in affine "Niels coordinates".
     * @return $P - Q$.
     */
    CafeCompletedPoint sub(CafeAffineNielsPoint q) {
        CafeFieldElement yAddX = y.add(x);
        CafeFieldElement ySubX = y.sub(x);
        CafeFieldElement pm = yAddX.mul(q.ySubX);
        CafeFieldElement mp = ySubX.mul(q.yAddX);
        CafeFieldElement txy2d = t.mul(q.xy2d);
        CafeFieldElement z2 = z.add(z);
        return new CafeCompletedPoint(pm.sub(mp), pm.add(mp), z2.sub(txy2d), z2.add(txy2d));
    }

    /**
     * Point negation.
     *
     * @return $-P$.
     */
    public CafeEdwardsPoint neg() {
        return new CafeEdwardsPoint(x.neg(), y, z, t.neg());
    }

    /**
     * Point doubling.
     *
     * @return $[2]P$.
     */
    public CafeEdwardsPoint dbl() {
        return toProjective().dbl().toExtended();
    }

    /**
     * Constant-time variable-base scalar multiplication.
     *
     * @param s the Scalar to multiply by.
     * @return $[s]P$
     */
    public CafeEdwardsPoint mul(final CafeScalar s) {
        // Construct a lookup table of [P,2P,3P,4P,5P,6P,7P,8P]
        final CafeProjectiveNielsPoint.LookupTable lookupTable = CafeProjectiveNielsPoint.buildLookupTable(this);

        /* Compute
         *
         * s = s_0 + s_1*16^1 + ... + s_63*16^63,
         *
         * with -8 ≤ s_i < 8 for 0 ≤ i < 63 and -8 ≤ s_63 ≤ 8.
         */
        final byte[] e = s.toRadix16();

        /*
         * Compute s*P as
         *
         *    s*P = P*(s_0 +   s_1*16^1 +   s_2*16^2 + ... +   s_63*16^63)
         *    s*P =  P*s_0 + P*s_1*16^1 + P*s_2*16^2 + ... + P*s_63*16^63
         *    s*P = P*s_0 + 16*(P*s_1 + 16*(P*s_2 + 16*( ... + P*s_63)...))
         *
         * We sum right-to-left.
         */
        CafeEdwardsPoint q = CafeEdwardsPoint.IDENTITY;
        for (int i = 63; i >= 0; i--) {
            q = q.pow2Mul(4);
            q = q.add(lookupTable.select(e[i])).toExtended();
        }
        return q;
    }

    /**
     * Compute $r = [a]A + [b]B$ in variable time, where $B$ is the Ed25519 base point.
     *
     * @param a      a Scalar.
     * @param pointA an EdwardsPoint.
     * @param b      a Scalar.
     * @return $[a]A + [b]B$.
     */
    public static CafeEdwardsPoint doubleScalarBaseMul(
        final CafeScalar a, final CafeEdwardsPoint pointA, final CafeScalar b) {
        final byte[] aNaf = a.nonAdjacentForm();
        final byte[] bNaf = b.nonAdjacentForm();

        CafeProjectiveNielsPoint.NafLookupTable tableA = CafeProjectiveNielsPoint.buildNafLookupTable(pointA);
        CafeAffineNielsPoint.NafLookupTable tableB = CafeConstants.AFFINE_ODD_MULTIPLES_OF_BASE_POINT;

        int i;
        for (i = 255; i >= 0; --i) {
            if (aNaf[i] != 0 || bNaf[i] != 0) {
                break;
            }
        }

        CafeProjectivePoint r = CafeEdwardsPoint.IDENTITY.toProjective();
        for (; i >= 0; --i) {
            CafeCompletedPoint t = r.dbl();

            if (aNaf[i] > 0) {
                t = t.toExtended().add(tableA.select(aNaf[i]));
            } else if (aNaf[i] < 0) {
                t = t.toExtended().sub(tableA.select(-aNaf[i]));
            }

            if (bNaf[i] > 0) {
                t = t.toExtended().add(tableB.select(bNaf[i]));
            } else if (bNaf[i] < 0) {
                t = t.toExtended().sub(tableB.select(-bNaf[i]));
            }

            r = t.toProjective();
        }

        return r.toExtended();
    }

    /**
     * Multiply by the cofactor.
     *
     * @return $[8]P$.
     */
    public CafeEdwardsPoint cofactorMul() {
        return pow2Mul(3);
    }

    /**
     * Compute $[2^k]P$ by successive doublings.
     *
     * @param k the exponent of 2. Must be positive and non-zero.
     * @return $[2^k]P$.
     */
    CafeEdwardsPoint pow2Mul(int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("Exponent must be positive and non-zero");
        }
        CafeProjectivePoint s = toProjective();
        for (int i = 0; i < k - 1; i++) {
            s = s.dbl().toProjective();
        }
        // Unroll last doubling, so we can go directly to extended coordinates.
        return s.dbl().toExtended();
    }

    /**
     * Determine if this point is the identity.
     *
     * @return true if this point is the identity, false otherwise.
     */
    public boolean isIdentity() {
        return this.cequals(CafeEdwardsPoint.IDENTITY) == 1;
    }

    /**
     * Determine if this point is in the 8-torsion subgroup $(\mathcal E[8])$, and therefore of small order.
     *
     * @return true if this point is of small order, false otherwise.
     */
    public boolean hasSmallOrder() {
        return cofactorMul().isIdentity();
    }

    /**
     * Determine if this point is contained in the prime-order subgroup $(\mathcal E[\ell])$, and has no torsion component.
     *
     * @return true if this point has zero torsion component and is in the prime-order subgroup, false otherwise.
     */
    public boolean isTorsionFree() {
        return this.mul(CafeConstants.BASE_POINT_ORDER).isIdentity();
    }

    @Override
    public String toString() {
        String ir = "EdwardsPoint(\n";
        ir += "    x: " + x.toString() + ",\n";
        ir += "    y: " + y.toString() + ",\n";
        ir += "    z: " + z.toString() + ",\n";
        ir += "    t: " + t.toString() + ",\n";
        ir += ")";
        return ir;
    }
}
