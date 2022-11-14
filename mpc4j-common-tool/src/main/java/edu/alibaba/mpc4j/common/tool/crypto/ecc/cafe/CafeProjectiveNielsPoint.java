/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

/**
 * A pre-computed point on the $\mathbb P^3$ model of the curve, represented as $(Y+X, Y-X, Z, 2dXY)$ in "Niels coordinates".
 * Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/ProjectiveNielsPoint.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/9
 */
class CafeProjectiveNielsPoint {
    /**
     * identity
     */
    private static final CafeProjectiveNielsPoint IDENTITY = new CafeProjectiveNielsPoint(
        CafeFieldElement.ONE, CafeFieldElement.ONE, CafeFieldElement.ONE, CafeFieldElement.ZERO
    );
    /**
     * lookup table size
     */
    private static final int LOOKUP_TABLE_SIZE = 8;
    /**
     * y + x
     */
    final CafeFieldElement yAddX;
    /**
     * y - x
     */
    final CafeFieldElement ySubX;
    /**
     * z
     */
    final CafeFieldElement z;
    /**
     * t * 2d
     */
    final CafeFieldElement t2d;

    CafeProjectiveNielsPoint(CafeFieldElement yAddX, CafeFieldElement ySubX, CafeFieldElement z, CafeFieldElement t2d) {
        this.yAddX = yAddX;
        this.ySubX = ySubX;
        this.z = z;
        this.t2d = t2d;
    }

    /**
     * Constant-time selection between two ProjectiveNielsPoints.
     *
     * @param that the other point.
     * @param c    must be 0 or 1, otherwise results are undefined.
     * @return a copy of this if $c == 0$, or a copy of that if $c == 1$.
     */
    public CafeProjectiveNielsPoint cmov(CafeProjectiveNielsPoint that, int c) {
        return new CafeProjectiveNielsPoint(
            yAddX.cmov(that.yAddX, c), ySubX.cmov(that.ySubX, c), z.cmov(that.z, c), t2d.cmov(that.t2d, c)
        );
    }

    /**
     * Point negation.
     *
     * @return $-P$.
     */
    public CafeProjectiveNielsPoint neg() {
        return new CafeProjectiveNielsPoint(ySubX, yAddX, z, t2d.neg());
    }

    /**
     * Construct a lookup table of $[point, [2]point, [3]point, [4]point, [5]point, [6]point, [7]point, [8]point]$.
     *
     * @param point the point to calculate multiples for.
     * @return the lookup table.
     */
    static LookupTable buildLookupTable(CafeEdwardsPoint point) {
        final CafeProjectiveNielsPoint[] points = new CafeProjectiveNielsPoint[LOOKUP_TABLE_SIZE];
        points[0] = point.toProjectiveNiels();
        for (int i = 1; i < LOOKUP_TABLE_SIZE; i++) {
            points[i] = point.add(points[i - 1]).toExtended().toProjectiveNiels();
        }
        return new LookupTable(points);
    }

    static class LookupTable {
        /**
         * precompute points $[P, [2]P, [3]P, [4]P, [5]P, [6]P, [7]P, [8]P]$
         */
        private final CafeProjectiveNielsPoint[] table;

        LookupTable(CafeProjectiveNielsPoint[] table) {
            this.table = table;
        }

        /**
         * Given $-8 \leq x \leq 8$, return $[x]P$ in constant time.
         *
         * @param x the index.
         * @return the pre-computed point.
         */
        CafeProjectiveNielsPoint select(final int x) {
            if (x < -LOOKUP_TABLE_SIZE || x > LOOKUP_TABLE_SIZE) {
                throw new IllegalArgumentException(
                    "x must be in range ]" + -LOOKUP_TABLE_SIZE + ", " + LOOKUP_TABLE_SIZE + "]: " + x
                );
            }

            // Is x negative?
            final int xNegative = CafeConstantTimeUtils.isNeg(x);
            // |x|
            final int xAbs = x - (((-xNegative) & x) << 1);

            // |x| P
            CafeProjectiveNielsPoint t = CafeProjectiveNielsPoint.IDENTITY;
            for (int i = 1; i < LOOKUP_TABLE_SIZE + 1; i++) {
                t = t.cmov(table[i - 1], CafeConstantTimeUtils.equal(xAbs, i));
            }

            // -|x| P
            final CafeProjectiveNielsPoint tNeg = t.neg();
            // [x]P
            return t.cmov(tNeg, xNegative);
        }
    }

    /**
     * Construct a lookup table of $[P, [3]P, [5]P, [7]P, [9]P, [11]P, [13]P, [15]P]$.
     *
     * @param point the point to calculate multiples for.
     * @return the lookup table.
     */
    static NafLookupTable buildNafLookupTable(CafeEdwardsPoint point) {
        CafeProjectiveNielsPoint[] points = new CafeProjectiveNielsPoint[LOOKUP_TABLE_SIZE];
        points[0] = point.toProjectiveNiels();
        CafeEdwardsPoint doublePoint = point.dbl();
        for (int i = 0; i < LOOKUP_TABLE_SIZE - 1; i++) {
            points[i + 1] = doublePoint.add(points[i]).toExtended().toProjectiveNiels();
        }
        return new NafLookupTable(points);
    }

    static class NafLookupTable {
        /**
         * precompute points $[P, [3]P, [5]P, [7]P, [9]P, [11]P, [13]P, [15]P]$
         */
        private final CafeProjectiveNielsPoint[] table;

        NafLookupTable(CafeProjectiveNielsPoint[] table) {
            this.table = table;
        }

        /**
         * Given public, odd $x$ with $0 \lt x \lt 2^4$, return $[x]A$.
         *
         * @param x the index.
         * @return the pre-computed point.
         */
        CafeProjectiveNielsPoint select(final int x) {
            if ((x % 2 == 0) || x >= LOOKUP_TABLE_SIZE * 2) {
                throw new IllegalArgumentException("invalid x");
            }

            return table[x / 2];
        }
    }
}
