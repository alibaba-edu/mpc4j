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
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Zp64 band linear solver random test. The following paper:
 * <p>
 * Patel, Sarvar, Joon Young Seo, and Kevin Yeo. Don’t be Dense: Efficient Keyword PIR for Sparse Databases. To appear
 * in USENIX Security 2023.
 * </p>
 * shows that for w ∈ [50, 60]:
 * <p>d1 = 128, ε = 0.38; </p>
 * <p>d1 = 512, ε = 0.18; </p>
 * <p>d1 = 1024, ε = 0.13; </p>
 *
 * @author Weiran Liu
 * @date 2023/8/4
 */
public class Zp64BandLinearSolverRandomTest {
    /**
     * test rounds
     */
    private static final int TEST_ROUND = 100;
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * Zp64 instance
     */
    private final Zp64 zp64;
    /**
     * band linear solver
     */
    private final Zp64BandLinearSolver bandLinearSolver;

    public Zp64BandLinearSolverRandomTest() {
        zp64 = Zp64Factory.createInstance(EnvType.STANDARD, 40);
        secureRandom = new SecureRandom();
        bandLinearSolver = new Zp64BandLinearSolver(zp64);
    }

    @Test
    public void testDimension128() {
        // d1 = 128, ε = 0.38, w ∈ [50, 60]
        int nRows = 128;
        double epsilon = 0.38;
        testDimension(nRows, epsilon);
    }

    @Test
    public void testDimension512() {
        // d1 = 512, ε = 0.18, w ∈ [50, 60]
        int nRows = 512;
        double epsilon = 0.18;
        testDimension(nRows, epsilon);
    }

    @Test
    public void testDimension1024() {
        // d1 = 1024, ε = 0.13 w ∈ [50, 60]
        int nRows = 1024;
        double epsilon = 0.13;
        testDimension(nRows, epsilon);
    }

    private void testDimension(int nRows, double epsilon) {
        int nColumns = (int) Math.ceil(nRows * (1 + epsilon));
        int w = 60;
        long[] x = new long[nColumns];
        SystemInfo systemInfo;
        for (int round = 0; round < TEST_ROUND; round++) {
            int[] ss = IntStream.range(0, nRows).map(iRow -> secureRandom.nextInt(nColumns - w)).toArray();
            long[][] bandA = IntStream.range(0, nRows)
                .mapToObj(iRow -> IntStream.range(0, w)
                    .mapToLong(i -> zp64.createRandom(secureRandom))
                    .toArray()
                )
                .toArray(long[][]::new);
            long[] b = IntStream.range(0, nRows)
                .mapToLong(iRow -> zp64.createRandom(secureRandom))
                .toArray();
            systemInfo = bandLinearSolver.freeSolve(
                IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
            assertCorrect(ss, bandA, b, x);
            systemInfo = bandLinearSolver.fullSolve(
                IntUtils.clone(ss), nColumns, LongUtils.clone(bandA), LongUtils.clone(b), x
            );
            Assert.assertEquals(SystemInfo.Consistent, systemInfo);
            assertCorrect(ss, bandA, b, x);
            Arrays.stream(x).forEach(element -> Assert.assertFalse(zp64.isZero(element)));
        }
    }

    private void assertCorrect(int[] ss, long[][] bandA, long[] b, long[] x) {
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
