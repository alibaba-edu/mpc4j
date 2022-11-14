/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import org.junit.Assert;
import org.junit.Test;

/**
 * Edwards precompute table test. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/test/java/cafe/cryptography/curve25519/EdwardsBasepointTableTest.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/9
 */
public class CafeEdwardsPrecomputeTableTest {
    @Test
    public void scalarMulVsEd25519py() {
        CafeEdwardsPrecomputeTable precomputeTable = new CafeEdwardsPrecomputeTable(CafeConstants.ED25519_BASE_POINT);
        CafeEdwardsPoint precomputeMulResult = precomputeTable.mul(CafeEdwardsPointTest.A_SCALAR);
        Assert.assertEquals(CafeEdwardsPointTest.A_MUL_BASE, precomputeMulResult.compress());
    }
}
