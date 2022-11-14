/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

/**
 * A pre-computed table of multiples of a Ristretto generator, for accelerating fixed-base scalar multiplication.
 * Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/RistrettoGeneratorTable.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/11
 */
public class CafeRistrettoGeneratorTable {
    /**
     * precompute table
     */
    final CafeEdwardsPrecomputeTable table;

    /**
     * Create a table of pre-computed multiples of generator.
     */
    public CafeRistrettoGeneratorTable(final CafeRistrettoPoint generator) {
        table = new CafeEdwardsPrecomputeTable(generator.repr);
    }

    /**
     * Constant-time fixed-base scalar multiplication.
     *
     * @param s the Scalar to multiply by.
     * @return $[s]B$.
     */
    public CafeRistrettoPoint mul(final CafeScalar s) {
        return new CafeRistrettoPoint(table.mul(s));
    }
}
