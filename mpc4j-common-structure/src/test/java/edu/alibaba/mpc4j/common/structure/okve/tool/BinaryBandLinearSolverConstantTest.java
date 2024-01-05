package edu.alibaba.mpc4j.common.structure.okve.tool;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * binary band linear solver constant test.
 *
 * @author Weiran Liu
 * @date 2023/8/5
 */
public class BinaryBandLinearSolverConstantTest {
    /**
     * l
     */
    private static final int L = 40;
    /**
     * byte L
     */
    private static final int BYTE_L = CommonUtils.getByteLength(L);
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * GF(2^e) instance
     */
    private final Gf2e gf2e;
    /**
     * binary band linear solver
     */
    private final BinaryBandLinearSolver bandLinearSolver;

    public BinaryBandLinearSolverConstantTest() {
        gf2e = Gf2eFactory.createInstance(EnvType.STANDARD, L);
        bandLinearSolver = new BinaryBandLinearSolver(L);
        secureRandom = new SecureRandom();
    }

    @Test
    public void test0x0() {
        int nRows = 0;
        int nColumns = 0;
        int w = 0;
        int byteW = 0;
        int[] ss = IntStream.range(0, nRows).map(iRow -> 0).toArray();
        byte[][] bandA = new byte[nRows][byteW];
        byte[][] b = new byte[nRows][BYTE_L];
        byte[][] x = new byte[nColumns][BYTE_L];
        SystemInfo systemInfo;
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
    }

    @Test
    public void test0xl() {
        int nRows = 0;
        int nColumns = 40;
        int w = 10;
        int byteW = CommonUtils.getByteLength(w);
        int[] ss = IntStream.range(0, nRows).map(iRow -> 0).toArray();
        byte[][] bandA = new byte[nRows][byteW];
        byte[][] b = new byte[nRows][BYTE_L];
        byte[][] x = new byte[nColumns][BYTE_L];
        SystemInfo systemInfo;
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        for (int iColumn = 0; iColumn < nColumns; iColumn++) {
            Assert.assertTrue(gf2e.isZero(x[iColumn]));
        }
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        for (int iColumn = 0; iColumn < nColumns; iColumn++) {
            Assert.assertFalse(gf2e.isZero(x[iColumn]));
        }
    }

    @Test
    public void test1x1() {
        int nColumns = 1;
        int w = 1;
        int[] ss;
        byte[][] bandA;
        byte[][] b;
        byte[][] x = new byte[nColumns][BYTE_L];
        SystemInfo systemInfo;

        // A = | 1 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new byte[][]{
            new byte[]{0b00000001,},
        };
        b = new byte[][]{
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));

        // A = | 1 |, b = r, solve Ax = b.
        bandA = new byte[][]{
            new byte[]{0b00000001,},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);

        // A = | 0 |, b = 0, solve Ax = b.
        bandA = new byte[][]{
            new byte[]{0b00000000,},
        };
        b = new byte[][]{
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[0]));

        // A = | 0 |, b = r, solve Ax = b.
        bandA = new byte[][]{
            new byte[]{0b00000000,},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void test1x2w2() {
        int nColumns = 2;
        int w = 2;
        int[] ss;
        byte[][] bandA;
        byte[][] b;
        byte[][] x = new byte[nColumns][BYTE_L];
        SystemInfo systemInfo;

        // A = | 1 1 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new byte[][]{
            new byte[]{0b00000011,},
        };
        b = new byte[][]{
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));

        // A = | 1 1 |, b = r, solve Ax = b.
        bandA = new byte[][]{
            new byte[]{0b00000011,},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));

        // A = | 0 1 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new byte[][]{
            new byte[]{0b00000001,},
        };
        b = new byte[][]{
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));

        // A = | 0 1 |, b = r, solve Ax = b.
        ss = new int[]{0,};
        bandA = new byte[][]{
            new byte[]{0b00000001,},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[0]));

        // A = | 1 0 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new byte[][]{
            new byte[]{0b00000010,},
        };
        b = new byte[][]{
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));

        // A = | 1 0 |, b = r, solve Ax = b.
        ss = new int[]{0,};
        bandA = new byte[][]{
            new byte[]{0b00000010,},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[1]));

        // A = | 0 0 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new byte[][]{
            new byte[]{0b00000000,},
        };
        b = new byte[][]{
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[0]));

        // A = | 0 0 |, b = r, solve Ax = b.
        ss = new int[]{0,};
        bandA = new byte[][]{
            new byte[]{0b00000000,},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void test1x2w1() {
        int nColumns = 2;
        int w = 1;
        int[] ss;
        byte[][] bandA;
        byte[][] b;
        byte[][] x = new byte[nColumns][BYTE_L];
        SystemInfo systemInfo;

        // A = | 0 1 |, b = 0, solve Ax = b.
        ss = new int[]{1,};
        bandA = new byte[][]{
            new byte[]{0b00000001,},
        };
        b = new byte[][]{
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));

        // A = | 0 1 |, b = r, solve Ax = b.
        ss = new int[]{1,};
        bandA = new byte[][]{
            new byte[]{0b00000001},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[0]));

        // A = | 1 0 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new byte[][]{
            new byte[]{0b00000001,},
        };
        b = new byte[][]{
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));

        // A = | 1 0 |, b = r, solve Ax = b.
        ss = new int[]{0,};
        bandA = new byte[][]{
            new byte[]{0b00000001,},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[1]));

        // s0 = 0, A = | 0 0 |, b = 0, solve Ax = b.
        ss = new int[]{0,};
        bandA = new byte[][]{
            new byte[]{0b00000000,},
        };
        b = new byte[][]{
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[0]));

        // s0 = 1, A = | 0 0 |, b = 0, solve Ax = b.
        ss = new int[]{1,};
        bandA = new byte[][]{
            new byte[]{0b00000000,},
        };
        b = new byte[][]{
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[0]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[0]));

        // s0 = 0, A = | 0 0 |, b = r, solve Ax = b.
        ss = new int[]{0,};
        bandA = new byte[][]{
            new byte[]{0b00000000,},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // s0 = 1, A = | 0 0 |, b = r, solve Ax = b.
        ss = new int[]{1,};
        bandA = new byte[][]{
            new byte[]{0b00000000,},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testAllOne2x2() {
        int nColumns = 2;
        int w = 2;
        int[] ss = new int[]{0, 0,};
        byte[][] bandA = new byte[][]{
            new byte[]{0b00000011,},
            new byte[]{0b00000011,},
        };
        byte[][] b;
        byte[][] x = new byte[nColumns][BYTE_L];
        SystemInfo systemInfo;

        // A = | 1 1 |, b = | 0 |, solve Ax = b.
        //     | 1 1 |      | 0 |
        b = new byte[][]{
            gf2e.createZero(),
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));

        // A = | 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 |      | 0  |
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 |, b = | 0  |, solve Ax = b.
        //     | 1 1 |      | r1 |
        b = new byte[][]{
            gf2e.createZero(),
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 |      | r1 |
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
            gf2e.createNonZeroRandom(secureRandom),
        };
        // r0 != r1
        if (!gf2e.isEqual(b[0], b[1])) {
            systemInfo = bandLinearSolver.freeSolve(
                IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
            systemInfo = bandLinearSolver.fullSolve(
                IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        }
        // r0 = r1
        b[1] = b[0];
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[1]));
    }

    @Test
    public void testAllZero2x2w2() {
        int w = 2;
        int[] ss = new int[]{0, 0};
        byte[][] bandA = new byte[][]{
            new byte[]{0b00000000,},
            new byte[]{0b00000000,},
        };
        testAllZero2x2(ss, w, bandA);
    }

    @Test
    public void testAllZero2x2w1() {
        int w = 1;
        int[] ss;
        byte[][] bandA = new byte[][]{
            new byte[]{0b00000000,},
            new byte[]{0b00000000,},
        };
        // s0 = 0, s1 = 0
        ss = new int[]{0, 0};
        testAllZero2x2(ss, w, bandA);
        // s0 = 0, s1 = 1
        ss = new int[]{0, 1};
        testAllZero2x2(ss, w, bandA);
        // s0 = 1, s1 = 0
        ss = new int[]{1, 0};
        testAllZero2x2(ss, w, bandA);
        // s0 = 1, s1 = 1
        ss = new int[]{1, 1};
        testAllZero2x2(ss, w, bandA);
    }

    private void testAllZero2x2(int[] ss, int w, byte[][] bandA) {
        int nColumns = 2;
        byte[][] b;
        byte[][] x = new byte[nColumns][BYTE_L];
        SystemInfo systemInfo;

        // A = | 0 0 |, b = | 0 |, solve Ax = b.
        //     | 0 0 |      | 0 |
        b = new byte[][]{
            gf2e.createZero(),
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 |      | 0  |
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 |, b = | 0  |, solve Ax = b.
        //     | 0 0 |      | r1 |
        b = new byte[][]{
            gf2e.createZero(),
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 |      | r1 |
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testSpecial2x2w2() {
        int w = 2;
        int[] ss = new int[]{0, 0,};
        int nColumns = 2;
        byte[][] bandA;
        byte[][] b;
        byte[][] x = new byte[nColumns][BYTE_L];
        SystemInfo systemInfo;

        // A = | 1 0 |, b = | r0 |, solve Ax = b.
        //     | 0 1 |      | r1 |
        bandA = new byte[][]{
            new byte[]{0b00000010,},
            new byte[]{0b00000001,},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
            gf2e.createNonZeroRandom(secureRandom),
        };
        if (!gf2e.isEqual(b[0], b[1])) {
            systemInfo = bandLinearSolver.freeSolve(
                IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
            assertCorrect(ss, w, bandA, b, x);
            systemInfo = bandLinearSolver.fullSolve(
                IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
            assertCorrect(ss, w, bandA, b, x);
        }
        b[1] = b[0];
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 1 0 |      | r1 |
        bandA = new byte[][]{
            new byte[]{0b00000000,},
            new byte[]{0b00000010,},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 1 |, b = | r0 |, solve Ax = b.
        //     | 1 0 |      | r1  |
        bandA = new byte[][]{
            new byte[]{0b00000001,},
            new byte[]{0b00000010,},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
    }

    @Test
    public void testSpecial2x2w1() {
        int w = 1;
        int[] ss;
        int nColumns = 2;
        byte[][] bandA;
        byte[][] b;
        byte[][] x = new byte[nColumns][BYTE_L];
        SystemInfo systemInfo;

        // A = | 1 0 |, b = | r0 |, solve Ax = b.
        //     | 0 1 |      | r1 |
        ss = new int[]{0, 1,};
        bandA = new byte[][]{
            new byte[]{0b00000001,},
            new byte[]{0b00000001,},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
            gf2e.createNonZeroRandom(secureRandom),
        };
        if (!gf2e.isEqual(b[0], b[1])) {
            systemInfo = bandLinearSolver.freeSolve(
                IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
            assertCorrect(ss, w, bandA, b, x);
            systemInfo = bandLinearSolver.fullSolve(
                IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
            assertCorrect(ss, w, bandA, b, x);
        }
        b[1] = b[0];
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);

        // s0 = 0, A = | 0 0 |, b = | r0 |, solve Ax = b.
        //             | 1 0 |      | r1 |
        ss = new int[]{0, 0};
        bandA = new byte[][]{
            new byte[]{0b00000000,},
            new byte[]{0b00000001,},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        // s0 = 1, A = | 0 0 |, b = | r0 |, solve Ax = b.
        //             | 1 0 |      | r1 |
        ss = new int[]{1, 0};
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 1 |, b = | r0 |, solve Ax = b.
        //     | 1 0 |      | r1  |
        ss = new int[]{1, 0};
        bandA = new byte[][]{
            new byte[]{0b00000001,},
            new byte[]{0b00000001,},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
    }

    @Test
    public void testAllOne2x3() {
        int w = 3;
        int[] ss = new int[]{0, 0,};
        int nColumns = 3;
        byte[][] bandA = new byte[][]{
            new byte[]{0b00000111,},
            new byte[]{0b00000111,},
        };
        byte[][] b;
        byte[][] x = new byte[nColumns][BYTE_L];
        SystemInfo systemInfo;

        // A = | 1 1 1 |, b = | 0 |, solve Ax = b.
        //     | 1 1 1 |      | 0 |
        b = new byte[][]{
            gf2e.createZero(),
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertFalse(gf2e.isZero(x[2]));

        // A = | 1 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | 0  |
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 1 |, b = | 0  |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        b = new byte[][]{
            gf2e.createZero(),
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
            gf2e.createNonZeroRandom(secureRandom),
        };
        // r0 != r1
        if (!gf2e.isEqual(b[0], b[1])) {
            systemInfo = bandLinearSolver.freeSolve(
                IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
            systemInfo = bandLinearSolver.fullSolve(
                IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        }
        // r0 = r1
        b[1] = b[0];
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertFalse(gf2e.isZero(x[2]));
    }

    @Test
    public void testAllZero2x3w3() {
        int w = 3;
        int[] ss = new int[]{0, 0,};
        byte[][] bandA = new byte[][]{
            new byte[]{0b00000000,},
            new byte[]{0b00000000,},
        };
        testAllZero2x3(ss, w, bandA);
    }

    @Test
    public void testAllZero2x3w2() {
        int w = 2;
        int[] ss;
        byte[][] bandA = new byte[][]{
            new byte[]{0b00000000,},
            new byte[]{0b00000000,},
        };
        // s0 = 0, s1 = 0
        ss = new int[]{0, 0,};
        testAllZero2x3(ss, w, bandA);
        // s0 = 0, s1 = 1
        ss = new int[]{0, 1,};
        testAllZero2x3(ss, w, bandA);
        // s0 = 1, s1 = 0
        ss = new int[]{1, 0,};
        testAllZero2x3(ss, w, bandA);
        // s0 = 1, s1 = 1
        ss = new int[]{1, 1,};
        testAllZero2x3(ss, w, bandA);
    }

    @Test
    public void testAllZero2x3w1() {
        int w = 1;
        int[] ss;
        byte[][] bandA = new byte[][]{
            new byte[]{0b00000000,},
            new byte[]{0b00000000,},
        };
        // s0 = 0, s1 = 0
        ss = new int[]{0, 0,};
        testAllZero2x3(ss, w, bandA);
        // s0 = 0, s1 = 1
        ss = new int[]{0, 1,};
        testAllZero2x3(ss, w, bandA);
        // s0 = 0, s1 = 2
        ss = new int[]{0, 2,};
        testAllZero2x3(ss, w, bandA);
        // s0 = 1, s1 = 0
        ss = new int[]{1, 0,};
        testAllZero2x3(ss, w, bandA);
        // s0 = 1, s1 = 1
        ss = new int[]{1, 1,};
        testAllZero2x3(ss, w, bandA);
        // s0 = 1, s1 = 2
        ss = new int[]{1, 2,};
        testAllZero2x3(ss, w, bandA);
        // s0 = 2, s1 = 0
        ss = new int[]{2, 0,};
        testAllZero2x3(ss, w, bandA);
        // s0 = 2, s1 = 1
        ss = new int[]{2, 1,};
        testAllZero2x3(ss, w, bandA);
        // s0 = 2, s1 = 2
        ss = new int[]{2, 2,};
        testAllZero2x3(ss, w, bandA);
    }

    private void testAllZero2x3(int[] ss, int w, byte[][] bandA) {
        int nColumns = 3;
        byte[][] b;
        byte[][] x = new byte[nColumns][BYTE_L];
        SystemInfo systemInfo;

        // A = | 0 0 0 |, b = | 0 |, solve Ax = b.
        //     | 0 0 0 |      | 0 |
        b = new byte[][]{
            gf2e.createZero(),
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertFalse(gf2e.isZero(x[2]));

        // A = | 0 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 0 |      | 0  |
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 0 |, b = | 0  |, solve Ax = b.
        //     | 0 0 0 |      | r1 |
        b = new byte[][]{
            gf2e.createZero(),
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 0 |      | r1 |
        b = new byte[][]{
            gf2e.createNonZeroRandom(secureRandom),
            gf2e.createNonZeroRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testSpecial2x3Case1w3() {
        int w = 3;
        int[] ss = new int[]{0, 0,};
        byte[][] bandA = new byte[][]{
            new byte[]{0b00000001,},
            new byte[]{0b00000100,},
        };
        testSpecial2x3Case1(ss, w, bandA);
    }

    @Test
    public void testSpecial2x3Case1w2() {
        int w = 2;
        int[] ss = new int[]{1, 0,};
        byte[][] bandA = new byte[][]{
            new byte[]{0b00000001,},
            new byte[]{0b00000010,},
        };
        testSpecial2x3Case1(ss, w, bandA);
    }

    @Test
    public void testSpecial2x3Case1w1() {
        int w = 1;
        int[] ss = new int[]{2, 0,};
        byte[][] bandA = new byte[][]{
            new byte[]{0b00000001,},
            new byte[]{0b00000001,},
        };
        testSpecial2x3Case1(ss, w, bandA);
    }

    private void testSpecial2x3Case1(int[] ss, int w, byte[][] bandA) {
        int nColumns = 3;
        byte[][] b;
        byte[][] x = new byte[nColumns][BYTE_L];
        SystemInfo systemInfo;

        // A = | 0 0 1 |, b = | 0 |, solve Ax = b.
        //     | 1 0 0 |      | 0 |
        b = new byte[][]{
            gf2e.createZero(),
            gf2e.createZero(),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isZero(x[2]));

        // A = | 0 0 1 |, b = | r0 |, solve Ax = b.
        //     | 1 0 0 |      | r1 |
        b = new byte[][]{
            gf2e.createRandom(secureRandom),
            gf2e.createRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[1]));
    }

    @Test
    public void testSpecial2x3Case2w3() {
        int w = 3;
        int[] ss = new int[]{0, 0,};
        byte[][] bandA = new byte[][]{
            new byte[]{0b00000011,},
            new byte[]{0b00000110,},
        };
        testSpecial2x3Case2(ss, w, bandA);
    }

    @Test
    public void testSpecial2x3Case2w2() {
        int w = 2;
        int[] ss = new int[]{1, 0,};
        byte[][] bandA = new byte[][]{
            new byte[]{0b00000011,},
            new byte[]{0b00000011,},
        };
        testSpecial2x3Case2(ss, w, bandA);
    }

    private void testSpecial2x3Case2(int[] ss, int w, byte[][] bandA) {
        int nColumns = 3;
        byte[][] b;
        byte[][] x = new byte[nColumns][BYTE_L];
        SystemInfo systemInfo;
        // A = | 0 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 0 |      | r1 |
        b = new byte[][]{
            gf2e.createRandom(secureRandom),
            gf2e.createRandom(secureRandom),
        };
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[2]));
    }

    @Test
    public void testSpecial2x3Case3() {
        int w = 3;
        int nColumns = 3;
        int[] ss = new int[]{0, 0,};
        byte[][] bandA = new byte[][]{
            new byte[]{0b00000011,},
            new byte[]{0b00000111,},
        };
        byte[][] b = new byte[][]{
            gf2e.createRandom(secureRandom),
            gf2e.createRandom(secureRandom),
        };
        byte[][] x = new byte[nColumns][BYTE_L];
        SystemInfo systemInfo;

        // A = | 0 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[2]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[2]));
    }

    @Test
    public void testSpecial4x7Case() {
        int w = 5;
        int nColumns = 7;
        int nRows = 4;
        int[] ss = new int[]{2, 0, 2, 2};
        byte[][] bandA = new byte[][]{
            new byte[]{0b00000100,},
            new byte[]{0b00011100,},
            new byte[]{0b00011100,},
            new byte[]{0b00010010,},
        };
        byte[][] b = IntStream.range(0, nRows)
            .mapToObj(iRow -> gf2e.createNonZeroRandom(secureRandom))
            .toArray(byte[][]::new);
        byte[][] x = new byte[nColumns][BYTE_L];
        SystemInfo systemInfo;

        // A = | 0 0 0 0 1 0 0 |, b = | 1 |, solve Ax = b.
        //     | 1 1 1 0 0 0 0 |      | 2 |
        //     | 0 0 1 1 1 0 0 |      | 3 |
        //     | 0 0 1 0 0 1 0 |      | 4 |
        systemInfo = bandLinearSolver.freeSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertTrue(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isZero(x[5]));
        Assert.assertTrue(gf2e.isZero(x[6]));
        systemInfo = bandLinearSolver.fullSolve(
            IntUtils.clone(ss), nColumns, w, BytesUtils.clone(bandA), BytesUtils.clone(b), x
        );
        Assert.assertEquals(SystemInfo.Consistent, systemInfo);
        assertCorrect(ss, w, bandA, b, x);
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertFalse(gf2e.isZero(x[5]));
        Assert.assertFalse(gf2e.isZero(x[6]));
    }

    private void assertCorrect(int[] ss, int w, byte[][] bandA, byte[][] b, byte[][] x) {
        int nRows = b.length;
        int byteW = CommonUtils.getByteLength(w);
        int offsetW = byteW * Byte.SIZE - w;
        for (int iRow = 0; iRow < nRows; iRow++) {
            byte[] result = gf2e.createZero();
            for (int iColumn = ss[iRow]; iColumn < ss[iRow] + w; iColumn++) {
                if (BinaryUtils.getBoolean(bandA[iRow], offsetW + iColumn - ss[iRow])) {
                    gf2e.addi(result, x[iColumn]);
                }
            }
            Assert.assertArrayEquals(b[iRow], result);
        }
    }
}
