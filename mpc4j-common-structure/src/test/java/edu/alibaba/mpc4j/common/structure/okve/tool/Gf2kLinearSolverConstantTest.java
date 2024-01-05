package edu.alibaba.mpc4j.common.structure.okve.tool;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * GF(2^κ) linear solver constant test.
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
public class Gf2kLinearSolverConstantTest {
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * GF(2^κ) instance
     */
    private final Gf2k gf2k;
    /**
     * GF(2^κ) linear solver
     */
    private final Gf2kLinearSolver linearSolver;

    public Gf2kLinearSolverConstantTest() {
        gf2k = Gf2kFactory.createInstance(EnvType.STANDARD);
        linearSolver = new Gf2kLinearSolver(gf2k);
    }

    @Test
    public void test0xl() {
        int m = gf2k.getL();
        byte[][][] matrixA = new byte[0][m][];
        byte[][] b = new byte[0][];
        byte[][] x = new byte[m][];
        SystemInfo systemInfo;
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        for (int iColumn = 0; iColumn < m; iColumn++) {
            Assert.assertTrue(gf2k.isZero(x[iColumn]));
        }
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        for (int iColumn = 0; iColumn < m; iColumn++) {
            Assert.assertFalse(gf2k.isZero(x[iColumn]));
        }
    }

    @Test
    public void test1x1() {
        byte[][][] matrixA;
        byte[][] b;
        byte[][] x = new byte[1][];
        SystemInfo systemInfo;

        // A = | 1 |, b = 0, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createOne(),},
        };
        b = new byte[][]{
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2k.isZero(x[0]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2k.isZero(x[0]));

        // A = | 1 |, b = r, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createOne(),},
        };
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2k.isEqual(b[0], x[0]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2k.isEqual(b[0], x[0]));

        // A = | 0 |, b = 0, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createZero(),},
        };
        b = new byte[][]{
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2k.isZero(x[0]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2k.isZero(x[0]));

        // A = | 0 |, b = r, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createZero(),},
        };
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testRandom1x1() {
        byte[][][] matrixA;
        byte[][] b;
        byte[][] x = new byte[1][];
        SystemInfo systemInfo;

        // A = | r[0] |, b = 0, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createNonZeroRandom(SECURE_RANDOM),},
        };
        b = new byte[][]{
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2k.isZero(x[0]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2k.isZero(x[0]));

        // A = | r[0] |, b = r, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createNonZeroRandom(SECURE_RANDOM),},
        };
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
    }

    @Test
    public void test1x2() {
        int m = 2;
        byte[][][] matrixA;
        byte[][] b;
        byte[][] x = new byte[m][];
        SystemInfo systemInfo;

        // A = | 1 1 |, b = 0, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createOne(), gf2k.createOne()},
        };
        b = new byte[][]{
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        Assert.assertTrue(gf2k.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[0]));
        Assert.assertFalse(gf2k.isZero(x[1]));

        // A = | 1 1 |, b = r, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createOne(), gf2k.createOne()},
        };
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[1]));

        // A = | 0 1 |, b = 0, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createZero(), gf2k.createOne()},
        };
        b = new byte[][]{
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        Assert.assertTrue(gf2k.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[0]));
        Assert.assertTrue(gf2k.isZero(x[1]));

        // A = | 0 1 |, b = r, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createZero(), gf2k.createOne()},
        };
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[0]));

        // A = | 1 0 |, b = 0, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createOne(), gf2k.createZero()},
        };
        b = new byte[][]{
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        Assert.assertTrue(gf2k.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        Assert.assertFalse(gf2k.isZero(x[1]));

        // A = | 1 0 |, b = r, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createOne(), gf2k.createZero()},
        };
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[1]));

        // A = | 0 0 |, b = 0, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createZero(), gf2k.createZero()},
        };
        b = new byte[][]{
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        Assert.assertTrue(gf2k.isZero(x[0]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[0]));
        Assert.assertFalse(gf2k.isZero(x[0]));

        // A = | 0 0 |, b = r, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createZero(), gf2k.createZero()},
        };
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testRandom1x2() {
        int m = 2;
        byte[][][] matrixA;
        byte[][] b;
        byte[][] x = new byte[m][];
        SystemInfo systemInfo;

        // A = | r[0] r[1] |, b = 0, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createNonZeroRandom(SECURE_RANDOM), gf2k.createNonZeroRandom(SECURE_RANDOM)},
        };
        b = new byte[][]{
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        Assert.assertTrue(gf2k.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[0]));
        Assert.assertFalse(gf2k.isZero(x[1]));

        // A = | r[0] r[1] |, b = r, solve Ax = b.
        matrixA = new byte[][][]{
            new byte[][]{gf2k.createNonZeroRandom(SECURE_RANDOM), gf2k.createNonZeroRandom(SECURE_RANDOM)},
        };
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[1]));
    }

    @Test
    public void testAllOne2x2() {
        int m = 2;
        byte[][][] matrixA = new byte[][][]{
            new byte[][]{gf2k.createOne(), gf2k.createOne()},
            new byte[][]{gf2k.createOne(), gf2k.createOne()},
        };
        byte[][] b;
        byte[][] x = new byte[m][];
        SystemInfo systemInfo;

        // A = | 1 1 |, b = | 0 |, solve Ax = b.
        //     | 1 1 |      | 0 |
        b = new byte[][]{
            gf2k.createZero(),
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        Assert.assertTrue(gf2k.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[0]));
        Assert.assertFalse(gf2k.isZero(x[1]));

        // A = | 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 |      | 0  |
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 |, b = | 0  |, solve Ax = b.
        //     | 1 1 |      | r1 |
        b = new byte[][]{
            gf2k.createZero(),
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 |      | r1 |
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        // r0 != r1
        if (!gf2k.isEqual(b[0], b[1])) {
            systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
            systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        }
        // r0 = r1
        b[1] = b[0];
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[1]));
    }

    @Test
    public void testAllZero2x2() {
        int m = 2;
        byte[][][] matrixA = new byte[][][]{
            new byte[][]{gf2k.createZero(), gf2k.createZero()},
            new byte[][]{gf2k.createZero(), gf2k.createZero()},
        };
        byte[][] b;
        byte[][] x = new byte[m][];
        SystemInfo systemInfo;

        // A = | 0 0 |, b = | 0 |, solve Ax = b.
        //     | 0 0 |      | 0 |
        b = new byte[][]{
            gf2k.createZero(),
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        Assert.assertTrue(gf2k.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[0]));
        Assert.assertFalse(gf2k.isZero(x[1]));

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 |      | 0  |
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 |, b = | 0  |, solve Ax = b.
        //     | 0 0 |      | r1 |
        b = new byte[][]{
            gf2k.createZero(),
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 |      | r1 |
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testSpecial2x2() {
        int m = 2;
        byte[][][] matrixA;
        byte[][] b;
        byte[][] x = new byte[m][];
        SystemInfo systemInfo;

        // A = | 1 0 |, b = | r0 |, solve Ax = b.
        //     | 0 1 |      | r1 |
        matrixA = new byte[][][]{
            new byte[][] {gf2k.createOne(), gf2k.createZero()},
            new byte[][] {gf2k.createZero(), gf2k.createOne()},
        };
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        if (!gf2k.isEqual(b[0], b[1])) {
            systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
            assertCorrect(matrixA, b, x);
            systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
            assertCorrect(matrixA, b, x);
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        }
        b[1] = b[0];
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 1 0 |      | r1 |
        matrixA = new byte[][][]{
            new byte[][] {gf2k.createZero(), gf2k.createZero()},
            new byte[][] {gf2k.createOne(), gf2k.createZero()},
        };
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 1 |, b = | r0 |, solve Ax = b.
        //     | 1 0 |      | r1  |
        matrixA = new byte[][][]{
            new byte[][] {gf2k.createZero(), gf2k.createOne()},
            new byte[][] {gf2k.createOne(), gf2k.createZero()},
        };
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
    }

    @Test
    public void testRandom2x2() {
        int m = 2;
        byte[][][] matrixA = new byte[][][]{
            new byte[][]{gf2k.createNonZeroRandom(SECURE_RANDOM), gf2k.createNonZeroRandom(SECURE_RANDOM)},
            new byte[][]{gf2k.createNonZeroRandom(SECURE_RANDOM), gf2k.createNonZeroRandom(SECURE_RANDOM)},
        };
        matrixA[1] = BytesUtils.clone(matrixA[0]);
        byte[][] b;
        byte[][] x = new byte[m][];
        SystemInfo systemInfo;

        // A = | r[0] r[1] |, b = | 0 |, solve Ax = b.
        //     | r[0] r[1] |      | 0 |
        b = new byte[][]{
            gf2k.createZero(),
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        Assert.assertTrue(gf2k.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[0]));
        Assert.assertFalse(gf2k.isZero(x[1]));

        // A = | r[0] r[1] |, b = | r0 |, solve Ax = b.
        //     | r[0] r[1] |      | 0  |
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | r[0] r[1] |, b = | 0  |, solve Ax = b.
        //     | r[0] r[1] |      | r1 |
        b = new byte[][]{
            gf2k.createZero(),
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | r[0] r[1] |, b = | r0 |, solve Ax = b.
        //     | r[0] r[1] |      | r1 |
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        // r0 != r1
        if (!gf2k.isEqual(b[0], b[1])) {
            systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
            systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        }
        // r0 == r1
        b[1] = b[0];
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[1]));
    }

    @Test
    public void testAllOne2x3() {
        int m = 3;
        byte[][][] matrixA = new byte[][][]{
            new byte[][] {gf2k.createOne(), gf2k.createOne(), gf2k.createOne()},
            new byte[][] {gf2k.createOne(), gf2k.createOne(), gf2k.createOne()},
        };
        byte[][] b;
        byte[][] x = new byte[m][];
        SystemInfo systemInfo;

        // A = | 1 1 1 |, b = | 0 |, solve Ax = b.
        //     | 1 1 1 |      | 0 |
        b = new byte[][]{
            gf2k.createZero(),
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        Assert.assertTrue(gf2k.isZero(x[1]));
        Assert.assertTrue(gf2k.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[0]));
        Assert.assertFalse(gf2k.isZero(x[1]));
        Assert.assertFalse(gf2k.isZero(x[2]));

        // A = | 1 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | 0  |
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 1 |, b = | 0  |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        b = new byte[][]{
            gf2k.createZero(),
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        // r0 != r1
        if (!gf2k.isEqual(b[0], b[1])) {
            systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
            systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        }
        // r0 = r1
        b[1] = b[0];
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[1]));
        Assert.assertTrue(gf2k.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[1]));
        Assert.assertFalse(gf2k.isZero(x[2]));
    }

    @Test
    public void testAllZero3x2() {
        int m = 3;
        byte[][][] matrixA = new byte[][][]{
            new byte[][] {gf2k.createZero(), gf2k.createZero(), gf2k.createZero()},
            new byte[][] {gf2k.createZero(), gf2k.createZero(), gf2k.createZero()},
        };
        byte[][] b;
        byte[][] x = new byte[m][];
        SystemInfo systemInfo;

        // A = | 0 0 0 |, b = | 0 |, solve Ax = b.
        //     | 0 0 0 |      | 0 |
        b = new byte[][]{
            gf2k.createZero(),
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        Assert.assertTrue(gf2k.isZero(x[1]));
        Assert.assertTrue(gf2k.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[0]));
        Assert.assertFalse(gf2k.isZero(x[1]));
        Assert.assertFalse(gf2k.isZero(x[2]));

        // A = | 0 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 0 |      | 0  |
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 0 |, b = | 0  |, solve Ax = b.
        //     | 0 0 0 |      | r1 |
        b = new byte[][]{
            gf2k.createZero(),
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 0 |      | r1 |
        b = new byte[][]{
            gf2k.createNonZeroRandom(SECURE_RANDOM),
            gf2k.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testSpecial2x3() {
        int m = 3;
        byte[][][] matrixA;
        byte[][] b;
        byte[][] x = new byte[m][];
        SystemInfo systemInfo;

        // A = | 0 0 1 |, b = | 0 |, solve Ax = b.
        //     | 1 0 0 |      | 0 |
        matrixA = new byte[][][]{
            new byte[][] {gf2k.createZero(), gf2k.createZero(), gf2k.createOne()},
            new byte[][] {gf2k.createOne(), gf2k.createZero(), gf2k.createZero()},
        };
        b = new byte[][]{
            gf2k.createZero(),
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        Assert.assertTrue(gf2k.isZero(x[1]));
        Assert.assertTrue(gf2k.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        Assert.assertFalse(gf2k.isZero(x[1]));
        Assert.assertTrue(gf2k.isZero(x[2]));

        // A = | 0 0 1 |, b = | r0 |, solve Ax = b.
        //     | 1 0 0 |      | r1 |
        matrixA = new byte[][][]{
            new byte[][] {gf2k.createZero(), gf2k.createZero(), gf2k.createOne()},
            new byte[][] {gf2k.createOne(), gf2k.createZero(), gf2k.createZero()},
        };
        b = new byte[][]{
            gf2k.createRandom(SECURE_RANDOM),
            gf2k.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[1]));

        // A = | 0 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 0 |      | r1 |
        matrixA = new byte[][][]{
            new byte[][] {gf2k.createZero(), gf2k.createOne(), gf2k.createOne()},
            new byte[][] {gf2k.createOne(), gf2k.createOne(), gf2k.createZero()},
        };
        b = new byte[][]{
            gf2k.createRandom(SECURE_RANDOM),
            gf2k.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[2]));

        // A = | 0 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        matrixA = new byte[][][]{
            new byte[][] {gf2k.createZero(), gf2k.createOne(), gf2k.createOne()},
            new byte[][] {gf2k.createOne(), gf2k.createOne(), gf2k.createOne()},
        };
        b = new byte[][]{
            gf2k.createRandom(SECURE_RANDOM),
            gf2k.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[2]));
    }

    @Test
    public void testRandomSpecial2x3() {
        int m = 3;
        byte[][][] matrixA;
        byte[][] b;
        byte[][] x = new byte[m][];
        SystemInfo systemInfo;

        // A = | 0 0 r |, b = | 0 |, solve Ax = b.
        //     | r 0 0 |      | 0 |
        matrixA = new byte[][][]{
            new byte[][] {gf2k.createZero(), gf2k.createZero(), gf2k.createNonZeroRandom(SECURE_RANDOM)},
            new byte[][] {gf2k.createNonZeroRandom(SECURE_RANDOM), gf2k.createZero(), gf2k.createZero()},
        };
        b = new byte[][]{
            gf2k.createZero(),
            gf2k.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        Assert.assertTrue(gf2k.isZero(x[1]));
        Assert.assertTrue(gf2k.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[0]));
        Assert.assertFalse(gf2k.isZero(x[1]));
        Assert.assertTrue(gf2k.isZero(x[2]));

        // A = | 0 0 r |, b = | r0 |, solve Ax = b.
        //     | r 0 0 |      | r1 |
        matrixA = new byte[][][]{
            new byte[][] {gf2k.createZero(), gf2k.createZero(), gf2k.createNonZeroRandom(SECURE_RANDOM)},
            new byte[][] {gf2k.createNonZeroRandom(SECURE_RANDOM), gf2k.createZero(), gf2k.createZero()},
        };
        b = new byte[][]{
            gf2k.createRandom(SECURE_RANDOM),
            gf2k.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[1]));

        // A = | 0 r r |, b = | r0 |, solve Ax = b.
        //     | r r 0 |      | r1 |
        matrixA = new byte[][][]{
            new byte[][] {gf2k.createZero(), gf2k.createNonZeroRandom(SECURE_RANDOM), gf2k.createNonZeroRandom(SECURE_RANDOM)},
            new byte[][] {gf2k.createNonZeroRandom(SECURE_RANDOM), gf2k.createNonZeroRandom(SECURE_RANDOM), gf2k.createZero()},
        };
        b = new byte[][]{
            gf2k.createRandom(SECURE_RANDOM),
            gf2k.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[2]));

        // A = | 0 r r |, b = | r0 |, solve Ax = b.
        //     | r r r |      | r1 |
        matrixA = new byte[][][]{
            new byte[][] {gf2k.createZero(), gf2k.createNonZeroRandom(SECURE_RANDOM), gf2k.createNonZeroRandom(SECURE_RANDOM)},
            new byte[][] {gf2k.createNonZeroRandom(SECURE_RANDOM), gf2k.createNonZeroRandom(SECURE_RANDOM), gf2k.createNonZeroRandom(SECURE_RANDOM)},
        };
        b = new byte[][]{
            gf2k.createRandom(SECURE_RANDOM),
            gf2k.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(gf2k.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), BytesUtils.clone(b), x);
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(gf2k.isZero(x[2]));
    }

    private void assertCorrect(byte[][][] matrixA, byte[][] b, byte[][] x) {
        int nRows = b.length;
        int nColumns = x.length;
        for (int iRow = 0; iRow < nRows; iRow++) {
            byte[] result = gf2k.createZero();
            for (int iColumn = 0; iColumn < nColumns; iColumn++) {
                gf2k.addi(result, gf2k.mul(matrixA[iRow][iColumn], x[iColumn]));
            }
            Assert.assertArrayEquals(b[iRow], result);
        }
    }
}
