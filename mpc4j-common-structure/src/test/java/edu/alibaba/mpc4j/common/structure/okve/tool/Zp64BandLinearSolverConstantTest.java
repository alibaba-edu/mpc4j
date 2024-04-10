package edu.alibaba.mpc4j.common.structure.okve.tool;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Zp64 band linear solver constant test.
 *
 * @author Weiran Liu
 * @date 2023/8/4
 */
public class Zp64BandLinearSolverConstantTest {
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * Zp64 instance
     */
    private final Zp64 zp64;
    /**
     * Small Zp64 instance
     */
    private final Zp64 smallZp64;
    /**
     * Zp linear solver
     */
    private final Zp64BandLinearSolver bandLinearSolver;
    /**
     * small Zp linear solver
     */
    private final Zp64BandLinearSolver smallBandLinearSolver;

    public Zp64BandLinearSolverConstantTest() {
        zp64 = Zp64Factory.createInstance(EnvType.STANDARD, 40);
        smallZp64 = Zp64Factory.createInstance(EnvType.STANDARD, 1);
        bandLinearSolver = new Zp64BandLinearSolver(zp64);
        smallBandLinearSolver = new Zp64BandLinearSolver(smallZp64);
    }

    @Test
    public void test0x0() {
        int nRows = 0;
        int nColumns = 0;
        int w = 0;
        int[] ss = IntStream.range(0, nRows).map(iRow -> 0).toArray();
        long[][] bandA = new long[nRows][w];
        long[] b = new long[nRows];
        long[] x = new long[nColumns];
        SystemInfo systemInfo;
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
    }

    @Test
    public void test0xl() {
        int nRows = 0;
        int nColumns = 40;
        int w = 10;
        int[] ss = IntStream.range(0, nRows).map(iRow -> 0).toArray();
        long[][] bandA = new long[nRows][w];
        long[] b = new long[nRows];
        long[] x = new long[nColumns];
        SystemInfo systemInfo;
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        for (int iColumn = 0; iColumn < nColumns; iColumn++) {
            Assert.assertTrue(zp64.isZero(x[iColumn]));
        }
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        for (int iColumn = 0; iColumn < nColumns; iColumn++) {
            Assert.assertFalse(zp64.isZero(x[iColumn]));
        }
    }

    @Test
    public void test1x1() {
        int nColumns = 1;
        int[] ss;
        long[][] bandA;
        long[] b;
        long[] x = new long[nColumns];
        SystemInfo systemInfo;

        // A = | 1 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new long[][]{
            new long[]{zp64.createOne(),},
        };
        b = new long[]{
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp64.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp64.isZero(x[0]));

        // A = | 1 |, b = r, solve Ax = b.
        bandA = new long[][]{
            new long[]{zp64.createOne(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);

        // A = | 0 |, b = 0, solve Ax = b.
        bandA = new long[][]{
            new long[]{zp64.createZero(),},
        };
        b = new long[]{
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp64.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(zp64.isZero(x[0]));

        // A = | 0 |, b = r, solve Ax = b.
        bandA = new long[][]{
            new long[]{zp64.createZero(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testRandom1x1() {
        int nColumns = 1;
        int[] ss;
        long[][] bandA;
        long[] b;
        long[] x = new long[nColumns];
        SystemInfo systemInfo;

        // A = | r[0] |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new long[][]{
            new long[]{zp64.createNonZeroRandom(SECURE_RANDOM),},
        };
        b = new long[]{
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp64.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp64.isZero(x[0]));

        // A = | r[0] |, b = r, solve Ax = b.
        bandA = new long[][]{
            new long[]{zp64.createNonZeroRandom(SECURE_RANDOM),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
    }

    @Test
    public void test1x2w2() {
        int nColumns = 2;
        int[] ss;
        long[][] bandA;
        long[] b;
        long[] x = new long[nColumns];
        SystemInfo systemInfo;

        // A = | 1 1 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new long[][]{
            new long[]{zp64.createOne(), zp64.createOne(),},
        };
        b = new long[]{
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[0]));
        Assert.assertFalse(zp64.isZero(x[1]));

        // A = | 1 1 |, b = r, solve Ax = b.
        bandA = new long[][]{
            new long[]{zp64.createOne(), zp64.createOne(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[0]));
        Assert.assertFalse(zp64.isZero(x[1]));

        // A = | 0 1 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new long[][]{
            new long[]{zp64.createZero(), zp64.createOne(),},
        };
        b = new long[]{
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[1]));

        // A = | 0 1 |, b = r, solve Ax = b.
        ss = new int[]{0,};
        bandA = new long[][]{
            new long[]{zp64.createZero(), zp64.createOne(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[0]));

        // A = | 1 0 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new long[][]{
            new long[]{zp64.createOne(), zp64.createZero(),},
        };
        b = new long[]{
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertFalse(zp64.isZero(x[1]));

        // A = | 1 0 |, b = r, solve Ax = b.
        ss = new int[]{0,};
        bandA = new long[][]{
            new long[]{zp64.createOne(), zp64.createZero(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[1]));

        // A = | 0 0 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new long[][]{
            new long[]{zp64.createZero(), zp64.createZero(),},
        };
        b = new long[]{
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[0]));
        Assert.assertFalse(zp64.isZero(x[0]));

        // A = | 0 0 |, b = r, solve Ax = b.
        ss = new int[]{0,};
        bandA = new long[][]{
            new long[]{zp64.createZero(), zp64.createZero(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void test1x2w1() {
        int nColumns = 2;
        int[] ss;
        long[][] bandA;
        long[] b;
        long[] x = new long[nColumns];
        SystemInfo systemInfo;

        // A = | 0 1 |, b = 0, solve Ax = b.
        ss = new int[]{1,};
        bandA = new long[][]{
            new long[]{zp64.createOne(),},
        };
        b = new long[]{
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[1]));

        // A = | 0 1 |, b = r, solve Ax = b.
        ss = new int[]{1,};
        bandA = new long[][]{
            new long[]{zp64.createOne(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[0]));

        // A = | 1 0 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new long[][]{
            new long[]{zp64.createOne(),},
        };
        b = new long[]{
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertFalse(zp64.isZero(x[1]));

        // A = | 1 0 |, b = r, solve Ax = b.
        ss = new int[]{0,};
        bandA = new long[][]{
            new long[]{zp64.createOne(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[1]));

        // s0 = 0, A = | 0 0 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new long[][]{
            new long[]{zp64.createZero(),},
        };
        b = new long[]{
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[0]));
        Assert.assertFalse(zp64.isZero(x[0]));

        // s0 = 1, A = | 0 0 |, b = 0, solve Ax = b.
        ss = new int[]{1,};
        bandA = new long[][]{
            new long[]{zp64.createZero(),},
        };
        b = new long[]{
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[0]));
        Assert.assertFalse(zp64.isZero(x[0]));

        // s0 = 0, A = | 0 0 |, b = r, solve Ax = b.
        ss = new int[]{0,};
        bandA = new long[][]{
            new long[]{zp64.createZero(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // s0 = 1, A = | 0 0 |, b = r, solve Ax = b.
        ss = new int[]{1,};
        bandA = new long[][]{
            new long[]{zp64.createZero(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testRandom1x2() {
        int nColumns = 2;
        int[] ss;
        long[][] bandA;
        long[] b;
        long[] x = new long[nColumns];
        SystemInfo systemInfo;

        // A = | r[0] r[1] |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new long[][]{
            new long[]{zp64.createNonZeroRandom(SECURE_RANDOM), zp64.createNonZeroRandom(SECURE_RANDOM),},
        };
        b = new long[]{
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[0]));
        Assert.assertFalse(zp64.isZero(x[1]));

        // A = | r[0] r[1] |, b = r, solve Ax = b.
        bandA = new long[][]{
            new long[]{zp64.createNonZeroRandom(SECURE_RANDOM), zp64.createNonZeroRandom(SECURE_RANDOM),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[1]));
    }

    @Test
    public void testAllOne2x2() {
        int nColumns = 2;
        int[] ss = new int[]{0, 0,};
        long[][] bandA = new long[][]{
            new long[]{zp64.createOne(), zp64.createOne(),},
            new long[]{zp64.createOne(), zp64.createOne(),},
        };
        long[] b;
        long[] x = new long[nColumns];
        SystemInfo systemInfo;

        // A = | 1 1 |, b = | 0 |, solve Ax = b.
        //     | 1 1 |      | 0 |
        b = new long[]{
            zp64.createZero(),
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[0]));
        Assert.assertFalse(zp64.isZero(x[1]));

        // A = | 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 |      | 0  |
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 |, b = | 0  |, solve Ax = b.
        //     | 1 1 |      | r1 |
        b = new long[]{
            zp64.createZero(),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 |      | r1 |
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        // r0 != r1
        if (!zp64.isEqual(b[0], b[1])) {
            systemInfo = bandLinearSolver.freeSolve(
                IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
            systemInfo = bandLinearSolver.fullSolve(
                IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        }
        // r0 = r1
        b[1] = b[0];
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[1]));
    }

    @Test
    public void testAllZero2x2w2() {
        int[] ss = new int[]{0, 0};
        long[][] bandA = new long[][]{
            new long[]{zp64.createZero(), zp64.createZero(),},
            new long[]{zp64.createZero(), zp64.createZero(),},
        };
        testAllZero2x2(ss, bandA);
    }

    @Test
    public void testAllZero2x2w1() {
        int[] ss;
        long[][] bandA = new long[][]{
            new long[]{zp64.createZero(),},
            new long[]{zp64.createZero(),},
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

    private void testAllZero2x2(int[] ss, long[][] bandA) {
        int nColumns = 2;
        long[] b;
        long[] x = new long[nColumns];
        SystemInfo systemInfo;

        // A = | 0 0 |, b = | 0 |, solve Ax = b.
        //     | 0 0 |      | 0 |
        b = new long[]{
            zp64.createZero(),
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[0]));
        Assert.assertFalse(zp64.isZero(x[1]));

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 |      | 0  |
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 |, b = | 0  |, solve Ax = b.
        //     | 0 0 |      | r1 |
        b = new long[]{
            zp64.createZero(),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 |      | r1 |
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testSpecial2x2w2() {
        int[] ss = new int[]{0, 0,};
        int nColumns = 2;
        long[][] bandA;
        long[] b;
        long[] x = new long[nColumns];
        SystemInfo systemInfo;

        // A = | 1 0 |, b = | r0 |, solve Ax = b.
        //     | 0 1 |      | r1 |
        bandA = new long[][]{
            new long[]{zp64.createOne(), zp64.createZero(),},
            new long[]{zp64.createZero(), zp64.createOne(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        if (!zp64.isEqual(b[0], b[1])) {
            systemInfo = bandLinearSolver.freeSolve(
                IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
            assertCorrect(ss, bandA, b, x, zp64);
            systemInfo = bandLinearSolver.fullSolve(
                IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
            assertCorrect(ss, bandA, b, x, zp64);
        }
        b[1] = b[0];
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 1 0 |      | r1 |
        bandA = new long[][]{
            new long[]{zp64.createZero(), zp64.createZero(),},
            new long[]{zp64.createOne(), zp64.createZero(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 1 |, b = | r0 |, solve Ax = b.
        //     | 1 0 |      | r1  |
        bandA = new long[][]{
            new long[]{zp64.createZero(), zp64.createOne(),},
            new long[]{zp64.createOne(), zp64.createZero(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
    }

    @Test
    public void testSpecial2x2w1() {
        int[] ss;
        int nColumns = 2;
        long[][] bandA;
        long[] b;
        long[] x = new long[nColumns];
        SystemInfo systemInfo;

        // A = | 1 0 |, b = | r0 |, solve Ax = b.
        //     | 0 1 |      | r1 |
        ss = new int[]{0, 1,};
        bandA = new long[][]{
            new long[]{zp64.createOne(),},
            new long[]{zp64.createOne(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        if (!zp64.isEqual(b[0], b[1])) {
            systemInfo = bandLinearSolver.freeSolve(
                IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
            assertCorrect(ss, bandA, b, x, zp64);
            systemInfo = bandLinearSolver.fullSolve(
                IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
            assertCorrect(ss, bandA, b, x, zp64);
        }
        b[1] = b[0];
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);

        // s0 = 0, A = | 0 0 |, b = | r0 |, solve Ax = b.
        //             | 1 0 |      | r1 |
        ss = new int[]{0, 0};
        bandA = new long[][]{
            new long[]{zp64.createZero(),},
            new long[]{zp64.createZero(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        // s0 = 1, A = | 0 0 |, b = | r0 |, solve Ax = b.
        //             | 1 0 |      | r1 |
        ss = new int[]{1, 0};
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 1 |, b = | r0 |, solve Ax = b.
        //     | 1 0 |      | r1  |
        ss = new int[]{1, 0};
        bandA = new long[][]{
            new long[]{zp64.createOne(),},
            new long[]{zp64.createOne(),},
        };
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
    }

    @Test
    public void testRandom2x2() {
        int nColumns = 2;
        int[] ss = new int[]{0, 0};
        long[][] bandA = new long[][]{
            new long[]{zp64.createNonZeroRandom(SECURE_RANDOM), zp64.createNonZeroRandom(SECURE_RANDOM),},
            new long[]{zp64.createNonZeroRandom(SECURE_RANDOM), zp64.createNonZeroRandom(SECURE_RANDOM),},
        };
        bandA[1] = LongUtils.clone(bandA[0]);
        long[] b;
        long[] x = new long[nColumns];
        SystemInfo systemInfo;

        // A = | r[0] r[1] |, b = | 0 |, solve Ax = b.
        //     | r[0] r[1] |      | 0 |
        b = new long[]{
            zp64.createZero(),
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[0]));
        Assert.assertFalse(zp64.isZero(x[1]));

        // A = | r[0] r[1] |, b = | r0 |, solve Ax = b.
        //     | r[0] r[1] |      | 0  |
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | r[0] r[1] |, b = | 0  |, solve Ax = b.
        //     | r[0] r[1] |      | r1 |
        b = new long[]{
            zp64.createZero(),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | r[0] r[1] |, b = | r0 |, solve Ax = b.
        //     | r[0] r[1] |      | r1 |
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        // r0 != r1
        if (!zp64.isEqual(b[0], b[1])) {
            systemInfo = bandLinearSolver.freeSolve(
                IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
            systemInfo = bandLinearSolver.fullSolve(
                IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        }
        // r0 == r1
        b[1] = b[0];
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[1]));
    }

    @Test
    public void testRandomSmallZp() {
        int nColumns = 10;
        int[] ss = new int[]{2, 2, 5, 3, 7};
        long[][] bandA = new long[][]{{2, 1}, {2, 1}, {1, 0}, {1, 1}, {1, 2}};
        long[] b = new long[]{0, 0, 0, 1, 2};
        long[] x = new long[nColumns];
        SystemInfo systemInfo;
        systemInfo = smallBandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, smallZp64);
        systemInfo = smallBandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, smallZp64);
    }

    @Test
    public void testAllOne2x3() {
        int[] ss = new int[]{0, 0,};
        int nColumns = 3;
        long[][] bandA = new long[][]{
            new long[]{zp64.createOne(), zp64.createOne(), zp64.createOne(),},
            new long[]{zp64.createOne(), zp64.createOne(), zp64.createOne(),},
        };
        long[] b;
        long[] x = new long[nColumns];
        SystemInfo systemInfo;

        // A = | 1 1 1 |, b = | 0 |, solve Ax = b.
        //     | 1 1 1 |      | 0 |
        b = new long[]{
            zp64.createZero(),
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[1]));
        Assert.assertTrue(zp64.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[0]));
        Assert.assertFalse(zp64.isZero(x[1]));
        Assert.assertFalse(zp64.isZero(x[2]));

        // A = | 1 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | 0  |
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 1 |, b = | 0  |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        b = new long[]{
            zp64.createZero(),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        // r0 != r1
        if (!zp64.isEqual(b[0], b[1])) {
            systemInfo = bandLinearSolver.freeSolve(
                IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
            systemInfo = bandLinearSolver.fullSolve(
                IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        }
        // r0 = r1
        b[1] = b[0];
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[1]));
        Assert.assertTrue(zp64.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[1]));
        Assert.assertFalse(zp64.isZero(x[2]));
    }

    @Test
    public void testAllZero2x3w3() {
        int[] ss = new int[]{0, 0,};
        long[][] bandA = new long[][]{
            new long[]{zp64.createZero(), zp64.createZero(), zp64.createZero(),},
            new long[]{zp64.createZero(), zp64.createZero(), zp64.createZero(),},
        };
        testAllZero2x3(ss, bandA);
    }

    @Test
    public void testAllZero2x3w2() {
        int[] ss;
        long[][] bandA = new long[][]{
            new long[]{zp64.createZero(), zp64.createZero(),},
            new long[]{zp64.createZero(), zp64.createZero(),},
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
        long[][] bandA = new long[][]{
            new long[]{zp64.createZero(),},
            new long[]{zp64.createZero(),},
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

    private void testAllZero2x3(int[] ss, long[][] bandA) {
        int nColumns = 3;
        long[] b;
        long[] x = new long[nColumns];
        SystemInfo systemInfo;

        // A = | 0 0 0 |, b = | 0 |, solve Ax = b.
        //     | 0 0 0 |      | 0 |
        b = new long[]{
            zp64.createZero(),
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[1]));
        Assert.assertTrue(zp64.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[0]));
        Assert.assertFalse(zp64.isZero(x[1]));
        Assert.assertFalse(zp64.isZero(x[2]));

        // A = | 0 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 0 |      | 0  |
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 0 |, b = | 0  |, solve Ax = b.
        //     | 0 0 0 |      | r1 |
        b = new long[]{
            zp64.createZero(),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 0 |      | r1 |
        b = new long[]{
            zp64.createNonZeroRandom(SECURE_RANDOM),
            zp64.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testSpecial2x3Case1w3() {
        int[] ss = new int[]{0, 0,};
        long[][] bandA = new long[][]{
            new long[]{zp64.createZero(), zp64.createZero(), zp64.createOne(),},
            new long[]{zp64.createOne(), zp64.createZero(), zp64.createZero(),},
        };
        testSpecial2x3Case1(ss, bandA);
    }

    @Test
    public void testSpecial2x3Case1w2() {
        int[] ss = new int[]{1, 0,};
        long[][] bandA = new long[][]{
            new long[]{zp64.createZero(), zp64.createOne(),},
            new long[]{zp64.createOne(), zp64.createZero(),},
        };
        testSpecial2x3Case1(ss, bandA);
    }

    @Test
    public void testSpecial2x3Case1w1() {
        int[] ss = new int[]{2, 0,};
        long[][] bandA = new long[][]{
            new long[]{zp64.createOne(),},
            new long[]{zp64.createOne(),},
        };
        testSpecial2x3Case1(ss, bandA);
    }

    private void testSpecial2x3Case1(int[] ss, long[][] bandA) {
        int nColumns = 3;
        long[] b;
        long[] x = new long[nColumns];
        SystemInfo systemInfo;

        // A = | 0 0 1 |, b = | 0 |, solve Ax = b.
        //     | 1 0 0 |      | 0 |
        b = new long[]{
            zp64.createZero(),
            zp64.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertTrue(zp64.isZero(x[1]));
        Assert.assertTrue(zp64.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[0]));
        Assert.assertFalse(zp64.isZero(x[1]));
        Assert.assertTrue(zp64.isZero(x[2]));

        // A = | 0 0 1 |, b = | r0 |, solve Ax = b.
        //     | 1 0 0 |      | r1 |
        b = new long[]{
            zp64.createRandom(SECURE_RANDOM),
            zp64.createRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[1]));
    }

    @Test
    public void testSpecial2x3Case2w3() {
        int[] ss = new int[]{0, 0,};
        long[][] bandA = new long[][]{
            new long[]{zp64.createZero(), zp64.createOne(), zp64.createOne(),},
            new long[]{zp64.createOne(), zp64.createOne(), zp64.createZero(),},
        };
        testSpecial2x3Case2(ss, bandA);
    }

    @Test
    public void testSpecial2x3Case2w2() {
        int[] ss = new int[]{1, 0,};
        long[][] bandA = new long[][]{
            new long[]{zp64.createOne(), zp64.createOne(),},
            new long[]{zp64.createOne(), zp64.createOne(),},
        };
        testSpecial2x3Case2(ss, bandA);
    }

    private void testSpecial2x3Case2(int[] ss, long[][] bandA) {
        int nColumns = 3;
        long[] b;
        long[] x = new long[nColumns];
        SystemInfo systemInfo;
        // A = | 0 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 0 |      | r1 |
        b = new long[]{
            zp64.createRandom(SECURE_RANDOM),
            zp64.createRandom(SECURE_RANDOM),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[2]));
    }

    @Test
    public void testSpecial2x3Case3() {
        int nColumns = 3;
        int[] ss = new int[]{0, 0,};
        long[][] bandA = new long[][]{
            new long[]{zp64.createZero(), zp64.createOne(), zp64.createOne(),},
            new long[]{zp64.createOne(), zp64.createOne(), zp64.createOne(),},
        };
        long[] b = new long[]{
            zp64.createRandom(SECURE_RANDOM),
            zp64.createRandom(SECURE_RANDOM),
        };
        long[] x = new long[nColumns];
        SystemInfo systemInfo;

        // A = | 0 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[2]));
    }

    @Test
    public void testSpecial4x7Case() {
        int nColumns = 7;
        int nRows = 4;
        int[] ss = new int[]{2, 0, 2, 2};
        long[][] bandA = new long[][]{
            new long[]{0L, 0L, 1L, 0L, 0L,},
            new long[]{1L, 1L, 1L, 0L, 0L,},
            new long[]{1L, 1L, 1L, 0L, 0L,},
            new long[]{1L, 0L, 0L, 1L, 0L,},
        };
        long[] b = IntStream.range(0, nRows)
            .mapToLong(iRow -> iRow + 1)
            .toArray();
        long[] x = new long[nColumns];
        SystemInfo systemInfo;

        // A = | 0 0 0 0 1 0 0 |, b = | 1 |, solve Ax = b.
        //     | 1 1 1 0 0 0 0 |      | 2 |
        //     | 0 0 1 1 1 0 0 |      | 3 |
        //     | 0 0 1 0 0 1 0 |      | 4 |
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertTrue(zp64.isZero(x[1]));
        Assert.assertTrue(zp64.isZero(x[5]));
        Assert.assertTrue(zp64.isZero(x[6]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, bandA, b, x, zp64);
        Assert.assertFalse(zp64.isZero(x[1]));
        Assert.assertFalse(zp64.isZero(x[5]));
        Assert.assertFalse(zp64.isZero(x[6]));
    }

    private void assertCorrect(int[] ss, long[][] bandA, long[] b, long[] x, Zp64 zp64) {
        int w = bandA[0].length;
        int nRows = b.length;
        for (int iRow = 0; iRow < nRows; iRow++) {
            long result = zp64.createZero();
            for (int iColumn = ss[iRow]; iColumn < ss[iRow] + w; iColumn++) {
                result = zp64.add(result, zp64.mul(x[iColumn], bandA[iRow][iColumn - ss[iRow]]));
            }
            Assert.assertEquals(b[iRow], result);
        }
    }
}
