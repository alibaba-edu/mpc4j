/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

/**
 * A point $((X:Z), (Y:T))$ on the $\mathbb P^1 \times \mathbb P^1$ model of the curve. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/CompletedPoint.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/9
 */
class CafeCompletedPoint {
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

    CafeCompletedPoint(CafeFieldElement x, CafeFieldElement y, CafeFieldElement z, CafeFieldElement t) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.t = t;
    }

    /**
     * Convert this point from the $\mathbb P^1 \times \mathbb P^1$ model to the $\mathbb P^2$ model.
     * <p>
     * This costs $3 \mathrm M$.
     * </p>
     *
     * @return the point in the $\mathbb P^3$ model.
     */
    CafeProjectivePoint toProjective() {
        return new CafeProjectivePoint(x.mul(t), y.mul(z), z.mul(t));
    }

    /**
     * Convert this point from the $\mathbb P^1 \times \mathbb P^1$ model to the $\mathbb P^3$ model.
     * <p>
     * This costs $4 \mathrm M$.
     * </p>
     *
     * @return the point in the $\mathbb P^3$ model.
     */
    CafeEdwardsPoint toExtended() {
        return new CafeEdwardsPoint(x.mul(t), y.mul(z), z.mul(t), x.mul(y));
    }
}
