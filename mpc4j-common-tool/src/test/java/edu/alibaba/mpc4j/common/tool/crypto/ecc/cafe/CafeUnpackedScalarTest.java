/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unpacked scalar test. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/test/java/cafe/cryptography/curve25519/UnpackedScalarTest.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/11
 */
public class CafeUnpackedScalarTest {
    /**
     * Note: x is 2^253-1 which is slightly larger than the largest scalar produced by this implementation (l-1), and
     * should verify there are no overflows for valid scalars
     * <p>
     * x = 2^253-1 = 14474011154664524427946373126085988481658748083205070504932198000989141204991
     * </p>
     * <p>
     * x = 7237005577332262213973186563042994240801631723825162898930247062703686954002 mod l
     * </p>
     * <p>
     * x = 5147078182513738803124273553712992179887200054963030844803268920753008712037*R mod l in Montgomery form
     * </p>
     */
    private static final CafeUnpackedScalar X = new CafeUnpackedScalar(new int[]{
        0x1fffffff, 0x1fffffff, 0x1fffffff, 0x1fffffff, 0x1fffffff, 0x1fffffff, 0x1fffffff, 0x1fffffff, 0x001fffff,
    });

    /**
     * x^2 = 3078544782642840487852506753550082162405942681916160040940637093560259278169 mod l
     */
    private static final CafeUnpackedScalar XX = new CafeUnpackedScalar(new int[]{
        0x00217559, 0x000b3401, 0x103ff43b, 0x1462a62c, 0x1d6f9f38, 0x18e7a42f, 0x09a3dcee, 0x008dbe18, 0x0006ce65,
    });

    /**
     * y = 6145104759870991071742105800796537629880401874866217824609283457819451087098
     */
    private static final CafeUnpackedScalar Y = new CafeUnpackedScalar(new int[]{
        0x1e1458fa, 0x165ba838, 0x1d787b36, 0x0e577f3a, 0x1d2baf06, 0x1d689a19, 0x1fff3047, 0x117704ab, 0x000d9601,
    });

    /**
     * x * y = 36752150652102274958925982391442301741
     */
    private static final CafeUnpackedScalar XY = new CafeUnpackedScalar(new int[]{
        0x0ba7632d, 0x017736bb, 0x15c76138, 0x0c69daa1, 0x000001ba, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    });

    /**
     * a = 2351415481556538453565687241199399922945659411799870114962672658845158063753
     */
    private static final CafeUnpackedScalar A = new CafeUnpackedScalar(new int[]{
        0x07b3be89, 0x02291b60, 0x14a99f03, 0x07dc3787, 0x0a782aae, 0x16262525, 0x0cfdb93f, 0x13f5718d, 0x000532da,
    });

    /**
     * b = 4885590095775723760407499321843594317911456947580037491039278279440296187236
     */
    private static final CafeUnpackedScalar B = new CafeUnpackedScalar(new int[]{
        0x15421564, 0x1e69fd72, 0x093d9692, 0x161785be, 0x1587d69f, 0x09d9dada, 0x130246c0, 0x0c0a8e72, 0x000acd25,
    });
    /**
     * a + b = 0
     */
    private static final CafeUnpackedScalar A_ADD_B = new CafeUnpackedScalar(new int[]{
        0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    });
    /**
     * a - b = 4702830963113076907131374482398799845891318823599740229925345317690316127506
     */
    private static final CafeUnpackedScalar A_SUB_B = new CafeUnpackedScalar(new int[]{
        0x0f677d12, 0x045236c0, 0x09533e06, 0x0fb86f0f, 0x14f0555c, 0x0c4c4a4a, 0x19fb727f, 0x07eae31a, 0x000a65b5,
    });
    /**
     * 0
     */
    private static final CafeUnpackedScalar ZERO = new CafeUnpackedScalar(new int[]{
        0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    });

    @Test
    public void testUnpackThenPack() {
        Assert.assertArrayEquals(CafeScalarTest.TV1_R, CafeUnpackedScalar.decode(CafeScalarTest.TV1_R).encode());
        Assert.assertArrayEquals(CafeScalarTest.TV1_H, CafeUnpackedScalar.decode(CafeScalarTest.TV1_H).encode());
        Assert.assertArrayEquals(CafeScalarTest.TV1_A, CafeUnpackedScalar.decode(CafeScalarTest.TV1_A).encode());
        Assert.assertArrayEquals(CafeScalarTest.TV1_S, CafeUnpackedScalar.decode(CafeScalarTest.TV1_S).encode());
    }

    @Test
    public void testAddModuleToZero() {
        Assert.assertArrayEquals(ZERO.s, ZERO.add(CafeConstants.L).s);
    }

    @Test
    public void testAdd() {
        Assert.assertArrayEquals(A_ADD_B.s, A.add(B).s);
    }

    @Test
    public void testSub() {
        Assert.assertArrayEquals(A_SUB_B.s, A.sub(B).s);
    }

    @Test
    public void testMul() {
        Assert.assertArrayEquals(XY.s, X.mul(Y).s);
    }

    @Test
    public void testMulMax() {
        Assert.assertArrayEquals(XX.s, X.mul(X).s);
    }
}
