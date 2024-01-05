package edu.alibaba.mpc4j.common.structure.okve.tool;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Zp band linear solver constant test.
 *
 * @author Weiran Liu
 * @date 2023/8/4
 */
public class ZpBandLinearSolverConstantTest {
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * Zp instance
     */
    private final Zp zp;
    /**
     * Zp linear solver
     */
    private final ZpBandLinearSolver bandLinearSolver;

    public ZpBandLinearSolverConstantTest() {
        zp = ZpFactory.createInstance(EnvType.STANDARD, 40);
        bandLinearSolver = new ZpBandLinearSolver(zp);
    }

    @Test
    public void test0x0() {
        int nRows = 0;
        int nColumns = 0;
        int w = 0;
        int[] ss = IntStream.range(0, nRows).map(iRow -> 0).toArray();
        BigInteger[][] bandA = new BigInteger[nRows][w];
        BigInteger[] b = new BigInteger[nRows];
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
    }

    @Test
    public void test0xl() {
        int nRows = 0;
        int nColumns = 40;
        int w = 10;
        int[] ss = IntStream.range(0, nRows).map(iRow -> 0).toArray();
        BigInteger[][] bandA = new BigInteger[nRows][w];
        BigInteger[] b = new BigInteger[nRows];
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        for (int iColumn = 0; iColumn < nColumns; iColumn++) {
            Assert.assertTrue(zp.isZero(x[iColumn]));
        }
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        for (int iColumn = 0; iColumn < nColumns; iColumn++) {
            Assert.assertFalse(zp.isZero(x[iColumn]));
        }
    }

    @Test
    public void test1x1() {
        int nColumns = 1;
        int[] ss;
        BigInteger[][] bandA;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;

        // A = | 1 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(),},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));

        // A = | 1 |, b = r, solve Ax = b.
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);

        // A = | 0 |, b = 0, solve Ax = b.
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(),},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(zp.isZero(x[0]));

        // A = | 0 |, b = r, solve Ax = b.
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testRandom1x1() {
        int nColumns = 1;
        int[] ss;
        BigInteger[][] bandA;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;

        // A = | r[0] |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createNonZeroRandom(SECURE_RANDOM),},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));

        // A = | r[0] |, b = r, solve Ax = b.
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createNonZeroRandom(SECURE_RANDOM),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
    }

    @Test
    public void test1x2w2() {
        int nColumns = 2;
        int[] ss;
        BigInteger[][] bandA;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;

        // A = | 1 1 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(), zp.createOne(),},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));

        // A = | 1 1 |, b = r, solve Ax = b.
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(), zp.createOne(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));

        // A = | 0 1 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createOne(),},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));

        // A = | 0 1 |, b = r, solve Ax = b.
        ss = new int[]{0,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createOne(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));

        // A = | 1 0 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(), zp.createZero(),},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));

        // A = | 1 0 |, b = r, solve Ax = b.
        ss = new int[]{0,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(), zp.createZero(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[1]));

        // A = | 0 0 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createZero(),},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[0]));

        // A = | 0 0 |, b = r, solve Ax = b.
        ss = new int[]{0,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createZero(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void test1x2w1() {
        int nColumns = 2;
        int[] ss;
        BigInteger[][] bandA;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;

        // A = | 0 1 |, b = 0, solve Ax = b.
        ss = new int[]{1,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(),},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));

        // A = | 0 1 |, b = r, solve Ax = b.
        ss = new int[]{1,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));

        // A = | 1 0 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(),},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));

        // A = | 1 0 |, b = r, solve Ax = b.
        ss = new int[]{0,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[1]));

        // s0 = 0, A = | 0 0 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(),},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[0]));

        // s0 = 1, A = | 0 0 |, b = 0, solve Ax = b.
        ss = new int[]{1,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(),},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[0]));

        // s0 = 0, A = | 0 0 |, b = r, solve Ax = b.
        ss = new int[]{0,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // s0 = 1, A = | 0 0 |, b = r, solve Ax = b.
        ss = new int[]{1,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testRandom1x2() {
        int nColumns = 2;
        int[] ss;
        BigInteger[][] bandA;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;

        // A = | r[0] r[1] |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createNonZeroRandom(SECURE_RANDOM), zp.createNonZeroRandom(SECURE_RANDOM),},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));

        // A = | r[0] r[1] |, b = r, solve Ax = b.
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createNonZeroRandom(SECURE_RANDOM), zp.createNonZeroRandom(SECURE_RANDOM),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[1]));
    }

    @Test
    public void testAllOne2x2() {
        int nColumns = 2;
        int[] ss = new int[]{0, 0,};
        BigInteger[][] bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(), zp.createOne(),},
            new BigInteger[]{zp.createOne(), zp.createOne(),},
        };
        BigInteger[] b;
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;

        // A = | 1 1 |, b = | 0 |, solve Ax = b.
        //     | 1 1 |      | 0 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));

        // A = | 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 |      | 0  |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 |, b = | 0  |, solve Ax = b.
        //     | 1 1 |      | r1 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 |      | r1 |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        // r0 != r1
        if (!zp.isEqual(b[0], b[1])) {
            systemInfo = bandLinearSolver.freeSolve(
                IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
            systemInfo = bandLinearSolver.fullSolve(
                IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        }
        // r0 = r1
        b[1] = b[0];
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[1]));
    }

    @Test
    public void testAllZero2x2w2() {
        int[] ss = new int[]{0, 0};
        BigInteger[][] bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createZero(),},
            new BigInteger[]{zp.createZero(), zp.createZero(),},
        };
        testAllZero2x2(ss, bandA);
    }

    @Test
    public void testAllZero2x2w1() {
        int[] ss;
        BigInteger[][] bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(),},
            new BigInteger[]{zp.createZero(),},
        };
        // s0 = 0, s1 = 0
        ss = new int[]{0, 0};
        testAllZero2x2(ss, bandA);
        // s0 = 0, s1 = 1
        ss = new int[]{0, 1};
        testAllZero2x2(ss, bandA);
        // s0 = 1, s1 = 0
        ss = new int[]{1, 0};
        testAllZero2x2(ss, bandA);
        // s0 = 1, s1 = 1
        ss = new int[]{1, 1};
        testAllZero2x2(ss, bandA);
    }

    private void testAllZero2x2(int[] ss, BigInteger[][] bandA) {
        int nColumns = 2;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;

        // A = | 0 0 |, b = | 0 |, solve Ax = b.
        //     | 0 0 |      | 0 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 |      | 0  |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 |, b = | 0  |, solve Ax = b.
        //     | 0 0 |      | r1 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 |      | r1 |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testSpecial2x2w2() {
        int[] ss = new int[]{0, 0,};
        int nColumns = 2;
        BigInteger[][] bandA;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;

        // A = | 1 0 |, b = | r0 |, solve Ax = b.
        //     | 0 1 |      | r1 |
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(), zp.createZero(),},
            new BigInteger[]{zp.createZero(), zp.createOne(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        if (!zp.isEqual(b[0], b[1])) {
            systemInfo = bandLinearSolver.freeSolve(
                IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
            assertCorrect(ss, bandA, b, x);
            systemInfo = bandLinearSolver.fullSolve(
                IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
            assertCorrect(ss, bandA, b, x);
        }
        b[1] = b[0];
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 1 0 |      | r1 |
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createZero(),},
            new BigInteger[]{zp.createOne(), zp.createZero(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 1 |, b = | r0 |, solve Ax = b.
        //     | 1 0 |      | r1  |
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createOne(),},
            new BigInteger[]{zp.createOne(), zp.createZero(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
    }

    @Test
    public void testSpecial2x2w1() {
        int[] ss;
        int nColumns = 2;
        BigInteger[][] bandA;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;

        // A = | 1 0 |, b = | r0 |, solve Ax = b.
        //     | 0 1 |      | r1 |
        ss = new int[]{0, 1,};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(),},
            new BigInteger[]{zp.createOne(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        if (!zp.isEqual(b[0], b[1])) {
            systemInfo = bandLinearSolver.freeSolve(
                IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
            assertCorrect(ss, bandA, b, x);
            systemInfo = bandLinearSolver.fullSolve(
                IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
            assertCorrect(ss, bandA, b, x);
        }
        b[1] = b[0];
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);

        // s0 = 0, A = | 0 0 |, b = | r0 |, solve Ax = b.
        //             | 1 0 |      | r1 |
        ss = new int[]{0, 0};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(),},
            new BigInteger[]{zp.createZero(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        // s0 = 1, A = | 0 0 |, b = | r0 |, solve Ax = b.
        //             | 1 0 |      | r1 |
        ss = new int[]{1, 0};
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 1 |, b = | r0 |, solve Ax = b.
        //     | 1 0 |      | r1  |
        ss = new int[]{1, 0};
        bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(),},
            new BigInteger[]{zp.createOne(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
    }

    @Test
    public void testRandom2x2() {
        int nColumns = 2;
        int[] ss = new int[]{0, 0};
        BigInteger[][] bandA = new BigInteger[][]{
            new BigInteger[]{zp.createNonZeroRandom(SECURE_RANDOM), zp.createNonZeroRandom(SECURE_RANDOM),},
            new BigInteger[]{zp.createNonZeroRandom(SECURE_RANDOM), zp.createNonZeroRandom(SECURE_RANDOM),},
        };
        bandA[1] = BigIntegerUtils.clone(bandA[0]);
        BigInteger[] b;
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;

        // A = | r[0] r[1] |, b = | 0 |, solve Ax = b.
        //     | r[0] r[1] |      | 0 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));

        // A = | r[0] r[1] |, b = | r0 |, solve Ax = b.
        //     | r[0] r[1] |      | 0  |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | r[0] r[1] |, b = | 0  |, solve Ax = b.
        //     | r[0] r[1] |      | r1 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | r[0] r[1] |, b = | r0 |, solve Ax = b.
        //     | r[0] r[1] |      | r1 |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        // r0 != r1
        if (!zp.isEqual(b[0], b[1])) {
            systemInfo = bandLinearSolver.freeSolve(
                IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
            systemInfo = bandLinearSolver.fullSolve(
                IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        }
        // r0 == r1
        b[1] = b[0];
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[1]));
    }

    @Test
    public void testAllOne2x3() {
        int[] ss = new int[]{0, 0,};
        int nColumns = 3;
        BigInteger[][] bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(), zp.createOne(), zp.createOne(),},
            new BigInteger[]{zp.createOne(), zp.createOne(), zp.createOne(),},
        };
        BigInteger[] b;
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;

        // A = | 1 1 1 |, b = | 0 |, solve Ax = b.
        //     | 1 1 1 |      | 0 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        Assert.assertTrue(zp.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));
        Assert.assertFalse(zp.isZero(x[2]));

        // A = | 1 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | 0  |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 1 |, b = | 0  |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        // r0 != r1
        if (!zp.isEqual(b[0], b[1])) {
            systemInfo = bandLinearSolver.freeSolve(
                IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
            systemInfo = bandLinearSolver.fullSolve(
                IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        }
        // r0 = r1
        b[1] = b[0];
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[1]));
        Assert.assertTrue(zp.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[1]));
        Assert.assertFalse(zp.isZero(x[2]));
    }

    @Test
    public void testAllZero2x3w3() {
        int[] ss = new int[]{0, 0,};
        BigInteger[][] bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createZero(), zp.createZero(),},
            new BigInteger[]{zp.createZero(), zp.createZero(), zp.createZero(),},
        };
        testAllZero2x3(ss, bandA);
    }

    @Test
    public void testAllZero2x3w2() {
        int[] ss;
        BigInteger[][] bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createZero(),},
            new BigInteger[]{zp.createZero(), zp.createZero(),},
        };
        // s0 = 0, s1 = 0
        ss = new int[]{0, 0,};
        testAllZero2x3(ss, bandA);
        // s0 = 0, s1 = 1
        ss = new int[]{0, 1,};
        testAllZero2x3(ss, bandA);
        // s0 = 1, s1 = 0
        ss = new int[]{1, 0,};
        testAllZero2x3(ss, bandA);
        // s0 = 1, s1 = 1
        ss = new int[]{1, 1,};
        testAllZero2x3(ss, bandA);
    }

    @Test
    public void testAllZero2x3w1() {
        int[] ss;
        BigInteger[][] bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(),},
            new BigInteger[]{zp.createZero(),},
        };
        // s0 = 0, s1 = 0
        ss = new int[]{0, 0,};
        testAllZero2x3(ss, bandA);
        // s0 = 0, s1 = 1
        ss = new int[]{0, 1,};
        testAllZero2x3(ss, bandA);
        // s0 = 0, s1 = 2
        ss = new int[]{0, 2,};
        testAllZero2x3(ss, bandA);
        // s0 = 1, s1 = 0
        ss = new int[]{1, 0,};
        testAllZero2x3(ss, bandA);
        // s0 = 1, s1 = 1
        ss = new int[]{1, 1,};
        testAllZero2x3(ss, bandA);
        // s0 = 1, s1 = 2
        ss = new int[]{1, 2,};
        testAllZero2x3(ss, bandA);
        // s0 = 2, s1 = 0
        ss = new int[]{2, 0,};
        testAllZero2x3(ss, bandA);
        // s0 = 2, s1 = 1
        ss = new int[]{2, 1,};
        testAllZero2x3(ss, bandA);
        // s0 = 2, s1 = 2
        ss = new int[]{2, 2,};
        testAllZero2x3(ss, bandA);
    }

    private void testAllZero2x3(int[] ss, BigInteger[][] bandA) {
        int nColumns = 3;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;

        // A = | 0 0 0 |, b = | 0 |, solve Ax = b.
        //     | 0 0 0 |      | 0 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        Assert.assertTrue(zp.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));
        Assert.assertFalse(zp.isZero(x[2]));

        // A = | 0 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 0 |      | 0  |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 0 |, b = | 0  |, solve Ax = b.
        //     | 0 0 0 |      | r1 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 0 |      | r1 |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testSpecial2x3Case1w3() {
        int[] ss = new int[]{0, 0,};
        BigInteger[][] bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createZero(), zp.createOne(),},
            new BigInteger[]{zp.createOne(), zp.createZero(), zp.createZero(),},
        };
        testSpecial2x3Case1(ss, bandA);
    }

    @Test
    public void testSpecial2x3Case1w2() {
        int[] ss = new int[]{1, 0,};
        BigInteger[][] bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createOne(),},
            new BigInteger[]{zp.createOne(), zp.createZero(),},
        };
        testSpecial2x3Case1(ss, bandA);
    }

    @Test
    public void testSpecial2x3Case1w1() {
        int[] ss = new int[]{2, 0,};
        BigInteger[][] bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(),},
            new BigInteger[]{zp.createOne(),},
        };
        testSpecial2x3Case1(ss, bandA);
    }

    private void testSpecial2x3Case1(int[] ss, BigInteger[][] bandA) {
        int nColumns = 3;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;

        // A = | 0 0 1 |, b = | 0 |, solve Ax = b.
        //     | 1 0 0 |      | 0 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        Assert.assertTrue(zp.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));
        Assert.assertTrue(zp.isZero(x[2]));

        // A = | 0 0 1 |, b = | r0 |, solve Ax = b.
        //     | 1 0 0 |      | r1 |
        b = new BigInteger[]{
            zp.createRandom(SECURE_RANDOM),
            zp.createRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[1]));
    }

    @Test
    public void testSpecial2x3Case2w3() {
        int[] ss = new int[]{0, 0,};
        BigInteger[][] bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createOne(), zp.createOne(),},
            new BigInteger[]{zp.createOne(), zp.createOne(), zp.createZero(),},
        };
        testSpecial2x3Case2(ss, bandA);
    }

    @Test
    public void testSpecial2x3Case2w2() {
        int[] ss = new int[]{1, 0,};
        BigInteger[][] bandA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(), zp.createOne(),},
            new BigInteger[]{zp.createOne(), zp.createOne(),},
        };
        testSpecial2x3Case2(ss, bandA);
    }

    private void testSpecial2x3Case2(int[] ss, BigInteger[][] bandA) {
        int nColumns = 3;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;
        // A = | 0 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 0 |      | r1 |
        b = new BigInteger[]{
            zp.createRandom(SECURE_RANDOM),
            zp.createRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[2]));
    }

    @Test
    public void testSpecial2x3Case3() {
        int nColumns = 3;
        int[] ss = new int[]{0, 0,};
        BigInteger[][] bandA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createOne(), zp.createOne(),},
            new BigInteger[]{zp.createOne(), zp.createOne(), zp.createOne(),},
        };
        BigInteger[] b = new BigInteger[]{
            zp.createRandom(SECURE_RANDOM),
            zp.createRandom(SECURE_RANDOM),
        };
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;

        // A = | 0 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[2]));
    }

    @Test
    public void testSpecial4x7Case() {
        int nColumns = 7;
        int nRows = 4;
        int[] ss = new int[]{2, 0, 2, 2};
        BigInteger[][] bandA = new BigInteger[][]{
            new BigInteger[]{BigInteger.valueOf(0), BigInteger.valueOf(0), BigInteger.valueOf(1), BigInteger.valueOf(0), BigInteger.valueOf(0),},
            new BigInteger[]{BigInteger.valueOf(1), BigInteger.valueOf(1), BigInteger.valueOf(1), BigInteger.valueOf(0), BigInteger.valueOf(0),},
            new BigInteger[]{BigInteger.valueOf(1), BigInteger.valueOf(1), BigInteger.valueOf(1), BigInteger.valueOf(0), BigInteger.valueOf(0),},
            new BigInteger[]{BigInteger.valueOf(1), BigInteger.valueOf(0), BigInteger.valueOf(0), BigInteger.valueOf(1), BigInteger.valueOf(0),},
        };
        BigInteger[] b = IntStream.range(0, nRows)
            .mapToObj(iRow -> BigInteger.valueOf(iRow + 1))
            .toArray(BigInteger[]::new);
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;

        // A = | 0 0 0 0 1 0 0 |, b = | 1 |, solve Ax = b.
        //     | 1 1 1 0 0 0 0 |      | 2 |
        //     | 0 0 1 1 1 0 0 |      | 3 |
        //     | 0 0 1 0 0 1 0 |      | 4 |
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertTrue(zp.isZero(x[1]));
        Assert.assertTrue(zp.isZero(x[5]));
        Assert.assertTrue(zp.isZero(x[6]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, BigIntegerUtils.clone(bandA), BigIntegerUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x);
        Assert.assertFalse(zp.isZero(x[1]));
        Assert.assertFalse(zp.isZero(x[5]));
        Assert.assertFalse(zp.isZero(x[6]));
    }

    private void assertCorrect(int[] ss, BigInteger[][] bandA, BigInteger[] b, BigInteger[] x) {
        int w = bandA[0].length;
        int nRows = b.length;
        for (int iRow = 0; iRow < nRows; iRow++) {
            BigInteger result = zp.createZero();
            for (int iColumn = ss[iRow]; iColumn < ss[iRow] + w; iColumn++) {
                result = zp.add(result, zp.mul(x[iColumn], bandA[iRow][iColumn - ss[iRow]]));
            }
            Assert.assertEquals(b[iRow], result);
        }
    }
}
