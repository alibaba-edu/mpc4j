/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

/**
 * A pre-computed table of multiples of a basepoint, for accelerating fixed-base scalar multiplication. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/EdwardsBasepointTable.java
 * </p>
 * @author Weiran Liu
 * @date 2022/11/9
 */
public class CafeEdwardsPrecomputeTable {
    /**
     * precompute lookup table size
     */
    private static final int PRECOMPUTE_TABLE_SIZE = 32;
    /**
     * lookup table, each contains 16 points.
     */
    private final CafeAffineNielsPoint.LookupTable[] tables;

    /**
     * Create a table of pre-computed multiples of the base point.
     *
     * @param basePoint the base point.
     */
    public CafeEdwardsPrecomputeTable(final CafeEdwardsPoint basePoint) {
        this.tables = new CafeAffineNielsPoint.LookupTable[PRECOMPUTE_TABLE_SIZE];
        CafeEdwardsPoint innerPoint = basePoint;
        for (int i = 0; i < PRECOMPUTE_TABLE_SIZE; i++) {
            this.tables[i] = CafeAffineNielsPoint.buildLookupTable(innerPoint);
            // Only every second sum is precomputed (16^2 = 256)
            innerPoint = innerPoint.pow2Mul(8);
        }
    }

    /**
     * Constant-time fixed-base scalar multiplication.
     *
     * @param s the Scalar to multiply by.
     * @return $[s]B$.
     */
    public CafeEdwardsPoint mul(final CafeScalar s) {
        int i;

        final byte[] e = s.toRadix16();

        CafeEdwardsPoint h = CafeEdwardsPoint.IDENTITY;
        for (i = 1; i < PRECOMPUTE_TABLE_SIZE * 2; i += 2) {
            h = h.add(tables[i / 2].select(e[i])).toExtended();
        }

        h = h.pow2Mul(4);

        for (i = 0; i < PRECOMPUTE_TABLE_SIZE * 2; i += 2) {
            h = h.add(tables[i / 2].select(e[i])).toExtended();
        }

        return h;
    }
}
