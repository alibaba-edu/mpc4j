package edu.alibaba.mpc4j.common.structure.okve.tool;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * binary band linear solver random test. The following paper:
 * <p>
 * Bienstock, Alexander, Sarvar Patel, Joon Young Seo, and Kevin Yeo. Near-Optimal Oblivious Key-Value Stores for
 * Efficient PSI, PSU and Volume-Hiding Multi-Maps. To appear in USENIX Security 2023.
 * </p>
 * provides some experiment results for choosing w based on n and ε. In our test case, we only consider n = 2^10, 2^14.
 * For n = 2^10, we have:
 * <p>ε = 0.03, 40 = 0.08047w − 3.464; </p>
 * <p>ε = 0.05, 40 = 0.13880w − 4.424; </p>
 * <p>ε = 0.07, 40 = 0.19470w − 5.383; </p>
 * <p>ε = 0.10, 40 = 0.27470w − 6.296; </p>
 * For n = 2^14, we have:
 * <p>ε = 0.03, 40 = 0.08253w − 5.751; </p>
 * <p>ε = 0.05, 40 = 0.13890w − 6.976; </p>
 * <p>ε = 0.07, 40 = 0.19260w − 8.150; </p>
 * <p>ε = 0.10, 40 = 0.26850w − 9.339; </p>
 *
 * @author Weiran Liu
 * @date 2023/8/4
 */
public class BinaryBandLinearSolverRandomTest {
    /**
     * test round
     */
    private static final int TEST_ROUND = 100;
    /**
     * GF(2^e) instance
     */
    private final Gf2e gf2e;
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * band linear solver
     */
    private final BinaryBandLinearSolver bandLinearSolver;

    public BinaryBandLinearSolverRandomTest() {
        gf2e = Gf2eFactory.createInstance(EnvType.STANDARD, 40);
        secureRandom = new SecureRandom();
        bandLinearSolver = new BinaryBandLinearSolver(gf2e.getL());
    }

    @Test
    public void testDimensionLog10Epsilon003() {
        // n = 2^10, ε = 0.03, 40 = 0.08047w − 3.464
        int nRows = 1 << 10;
        double epsilon = 0.03;
        int w = (int) Math.ceil((40 + 3.464) / 0.0847);
        test(nRows, epsilon, w);
    }

    @Test
    public void testDimensionLog10Epsilon005() {
        // n = 2^10, ε = 0.05, 40 = 0.13880w − 4.424
        int nRows = 1 << 10;
        double epsilon = 0.05;
        int w = (int) Math.ceil((40 + 4.424) / 0.13880);
        test(nRows, epsilon, w);
    }

    @Test
    public void testDimensionLog10Epsilon007() {
        // n = 2^10, ε = 0.07, 40 = 0.19470w − 5.383
        int nRows = 1 << 10;
        double epsilon = 0.07;
        int w = (int) Math.ceil((40 + 5.383) / 0.19470);
        test(nRows, epsilon, w);
    }

    @Test
    public void testDimensionLog10Epsilon010() {
        // n = 2^10, ε = 0.10, 40 = 0.27470w − 6.296
        int nRows = 1 << 10;
        double epsilon = 0.10;
        int w = (int) Math.ceil((40 + 6.296) / 0.27470);
        test(nRows, epsilon, w);
    }

    @Test
    public void testDimensionLog14Epsilon003() {
        // n = 2^14, ε = 0.03, 40 = 0.08253w − 5.751
        int nRows = 1 << 14;
        double epsilon = 0.03;
        int w = (int) Math.ceil((40 + 5.751) / 0.08253);
        test(nRows, epsilon, w);
    }

    @Test
    public void testDimensionLog14Epsilon005() {
        // n = 2^14, ε = 0.05, 40 = 0.13890w − 6.976
        int nRows = 1 << 14;
        double epsilon = 0.05;
        int w = (int) Math.ceil((40 + 6.976) / 0.13890);
        test(nRows, epsilon, w);
    }

    @Test
    public void testDimensionLog14Epsilon007() {
        // n = 2^14, ε = 0.07, 40 = 0.19260w − 8.150
        int nRows = 1 << 14;
        double epsilon = 0.07;
        int w = (int) Math.ceil((40 + 8.150) / 0.19260);
        test(nRows, epsilon, w);
    }

    @Test
    public void testDimensionLog14Epsilon010() {
        // n = 2^14, ε = 0.10, 40 = 0.26850w − 9.339
        int nRows = 1 << 14;
        double epsilon = 0.10;
        int w = (int) Math.ceil((40 + 9.339) / 0.26850);
        test(nRows, epsilon, w);
    }

    private void test(int nRows, double epsilon, int w) {
        int nColumns = (int) Math.ceil(nRows * (1 + epsilon));
        int byteW = CommonUtils.getByteLength(w);
        byte[][] x = new byte[nColumns][];
        SystemInfo systemInfo;
        for (int round = 0; round < TEST_ROUND; round++) {
            int[] ss = IntStream.range(0, nRows).map(iRow -> secureRandom.nextInt(nColumns - w)).toArray();
            byte[][] bandA = IntStream.range(0, nRows)
                .mapToObj(iRow -> BytesUtils.randomByteArray(byteW, w, secureRandom))
                .toArray(byte[][]::new);
            byte[][] b = IntStream.range(0, nRows)
                .mapToObj(iRow -> gf2e.createRandom(secureRandom))
                .toArray(byte[][]::new);
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
            Arrays.stream(x).forEach(element -> Assert.assertFalse(gf2e.isZero(element)));
        }
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
