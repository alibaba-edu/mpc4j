package edu.alibaba.mpc4j.common.structure.okve.tool;

import cc.redberry.rings.linear.LinearSolver;
import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.matrix.zp.DenseZpMatrix;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Zp linear solver random test.
 *
 * @author Weiran Liu
 * @date 2021/05/08
 */
@RunWith(Parameterized.class)
public class ZpLinearSolverRandomTest {
    /**
     * test round
     */
    private static final int TEST_ROUND = 50;
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        int[] ds = new int[]{7, 8, 9, 15, 16, 17, 39, 40, 41};
        int[] ls = new int[]{39, 40, 41};
        // add each l and d
        for (int l : ls) {
            for (int d : ds) {
                configurations.add(new Object[]{"l = " + l + ", D = " + d + ")", l, d});
            }
        }

        return configurations;
    }

    /**
     * Zp instance
     */
    private final Zp zp;
    /**
     * dimension d
     */
    private final int d;
    /**
     * Zp linear solver
     */
    private final ZpLinearSolver linearSolver;

    public ZpLinearSolverRandomTest(String name, int l, int d) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        zp = ZpFactory.createInstance(EnvType.STANDARD, l);
        this.d = d;
        linearSolver = new ZpLinearSolver(zp);
    }

    @Test
    public void testIdentitySquareFullRank() {
        List<BigInteger[]> identityRows = IntStream.range(0, d)
            .mapToObj(rowIndex -> {
                BigInteger[] row = new BigInteger[d];
                Arrays.fill(row, zp.createZero());
                row[rowIndex] = zp.createNonZeroRandom(SECURE_RANDOM);
                return row;
            })
            .collect(Collectors.toList());
        for (int round = 0; round < TEST_ROUND; round++) {
            Collections.shuffle(identityRows, SECURE_RANDOM);
            BigInteger[][] matrixA = identityRows.toArray(new BigInteger[0][]);
            BigInteger[] b = new BigInteger[d];
            for (int iRow = 0; iRow < d; iRow++) {
                b[iRow] = zp.createRandom(SECURE_RANDOM);
            }
            testGaussianElimination(matrixA, b);
        }
    }

    @Test
    public void testIdentitySquareNotFullRank() {
        List<BigInteger[]> identityRows = IntStream.range(0, d)
            .mapToObj(rowIndex -> {
                BigInteger[] row = new BigInteger[d];
                Arrays.fill(row, zp.createZero());
                row[rowIndex] = zp.createNonZeroRandom(SECURE_RANDOM);
                return row;
            })
            .collect(Collectors.toList());
        for (int round = 0; round < TEST_ROUND; round++) {
            Collections.shuffle(identityRows, SECURE_RANDOM);
            BigInteger[][] matrixA = BigIntegerUtils.clone(identityRows.toArray(new BigInteger[0][]));
            BigInteger[] b = new BigInteger[d];
            for (int iRow = 0; iRow < d; iRow++) {
                b[iRow] = zp.createRandom(SECURE_RANDOM);
            }
            // set a random row to be 0
            int r = SECURE_RANDOM.nextInt(d);
            Arrays.fill(matrixA[r], zp.createZero());
            b[r] = zp.createZero();
            testGaussianElimination(matrixA, b);
        }
    }

    @Test
    public void testRandomSquareFullRank() {
        for (int round = 0; round < TEST_ROUND; round++) {
            // we choose a full rank matrix
            BigInteger[][] matrixA = new BigInteger[d][d];
            for (int iRow = 0; iRow < d; iRow++) {
               for (int iColumn = 0; iColumn < d; iColumn++) {
                   matrixA[iRow][iColumn] = zp.createRandom(SECURE_RANDOM);
               }
            }
            try {
                DenseZpMatrix zpMatrix = DenseZpMatrix.fromDense(zp, matrixA);
                zpMatrix.inverse();
            } catch (ArithmeticException e) {
                continue;
            }
            BigInteger[] b = new BigInteger[d];
            for (int iRow = 0; iRow < d; iRow++) {
                b[iRow] = zp.createRandom(SECURE_RANDOM);
            }
            testGaussianElimination(matrixA, b);
        }
    }

    @Test
    public void testRandomSquareNotFullRank() {
        for (int round = 0; round < TEST_ROUND; round++) {
            // we choose a full rank matrix
            BigInteger[][] matrixA = new BigInteger[d][d];
            for (int iRow = 0; iRow < d; iRow++) {
                for (int iColumn = 0; iColumn < d; iColumn++) {
                    matrixA[iRow][iColumn] = zp.createRandom(SECURE_RANDOM);
                }
            }
            try {
                DenseZpMatrix zpMatrix = DenseZpMatrix.fromDense(zp, matrixA);
                zpMatrix.inverse();
            } catch (ArithmeticException e) {
                continue;
            }
            BigInteger[] b = new BigInteger[d];
            for (int iRow = 0; iRow < d; iRow++) {
                b[iRow] = zp.createRandom(SECURE_RANDOM);
            }
            // set a random row to be 0
            int r = SECURE_RANDOM.nextInt(d);
            Arrays.fill(matrixA[r], zp.createZero());
            b[r] = zp.createZero();
            testGaussianElimination(matrixA, b);
        }
    }

    @Test
    public void testRectangular() {
        for (int round = 0; round < TEST_ROUND; round++) {
            BigInteger[][] matrixA = new BigInteger[d][d * 2];
            for (int iRow = 0; iRow < d; iRow++) {
                Arrays.fill(matrixA[iRow], zp.createZero());
                // the left-most and the right-most bits are set to non-zero elements
                matrixA[iRow][iRow] = zp.createRandom(SECURE_RANDOM);
                matrixA[iRow][2 * d - 1 - iRow] = zp.createRandom(SECURE_RANDOM);
            }
            BigInteger[] b = new BigInteger[d];
            for (int iRow = 0; iRow < d; iRow++) {
                b[iRow] = zp.createRandom(SECURE_RANDOM);
            }
            testGaussianElimination(matrixA, b);
        }
    }

    private void testGaussianElimination(BigInteger[][] matrixA, BigInteger[] b) {
        int nColumns = matrixA[0].length;
        BigInteger[] x = new BigInteger[nColumns];
        SystemInfo systemInfo;
        // free solve
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertNotEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        assertCorrect(matrixA, b, x);
        // full solve
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertNotEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        assertCorrect(matrixA, b, x);
        for (BigInteger xi : x) {
            Assert.assertFalse(zp.isZero(xi));
        }
    }

    private void assertCorrect(BigInteger[][] matrixA, BigInteger[] b, BigInteger[] x) {
        int nRows = b.length;
        int nColumns = x.length;
        for (int iRow = 0; iRow < nRows; iRow++) {
            BigInteger result = zp.createZero();
            for (int iColumn = 0; iColumn < nColumns; iColumn++) {
                result = zp.add(result, zp.mul(matrixA[iRow][iColumn], x[iColumn]));
            }
            Assert.assertEquals(b[iRow], result);
        }
    }
}
