/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

/**
 * A point $(X:Y:Z)$ on the $\mathbb P^2$ model of the curve. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/ProjectivePoint.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/9
 */
class CafeProjectivePoint {
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

    CafeProjectivePoint(CafeFieldElement x, CafeFieldElement y, CafeFieldElement z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Convert this point from the $\mathbb P^2$ model to the $\mathbb P^3$ model.
     * <p>
     * This costs $3 \mathrm M + 1 \mathrm S$.
     * </p>
     *
     * @return the point in the $\mathbb P^3$ model.
     */
    CafeEdwardsPoint toExtended() {
        return new CafeEdwardsPoint(x.mul(z), y.mul(z), z.sqr(), x.mul(y));
    }

    /**
     * Point doubling: add this point to itself.
     *
     * @return $[2]P$ as a CompletedPoint.
     */
    CafeCompletedPoint dbl() {
        CafeFieldElement xx = this.x.sqr();
        CafeFieldElement yy = this.y.sqr();
        CafeFieldElement zz2 = this.z.sqrDbl();
        CafeFieldElement xAddY = this.x.add(this.y);
        CafeFieldElement xAddYsq = xAddY.sqr();
        CafeFieldElement yyAddXx = yy.add(xx);
        CafeFieldElement yySubXx = yy.sub(xx);
        return new CafeCompletedPoint(xAddYsq.sub(yyAddXx), yyAddXx, yySubXx, zz2.sub(yySubXx));
    }
}
