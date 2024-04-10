package edu.alibaba.mpc4j.common.structure.okve.tool;

import cc.redberry.rings.linear.LinearSolver;
import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * ECC linear solver random test.
 *
 * @author Weiran Liu
 * @date 2021/05/08
 */
public class EccLinearSolverRandomTest {
    /**
     * test round
     */
    private static final int RANDOM_ROUND = 200;
    /**
     * dimension
     */
    private static final int DIMENSION = 7;
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * ECC
     */
    private final Ecc ecc;
    /**
     * ECC order
     */
    private final BigInteger n;
    /**
     * ECC linear solver
     */
    private final EccLinearSolver eccLinearSolver;

    public EccLinearSolverRandomTest() {
        ecc = EccFactory.createInstance(EnvType.STANDARD);
        n = ecc.getN();
        eccLinearSolver = new EccLinearSolver(ecc);
    }

    @Test
    public void testFullRankLinearSolver() {
        for (int round = 0; round < RANDOM_ROUND; round++) {
            // with very high probability to have a solution
            BigInteger[][] matrixA = new BigInteger[DIMENSION][DIMENSION];
            for (int row = 0; row < DIMENSION; row++) {
                for (int column = 0; column < DIMENSION; column++) {
                    matrixA[row][column] = BigIntegerUtils.randomNonNegative(n, SECURE_RANDOM);
                }
            }
            ECPoint[] b = new ECPoint[DIMENSION];
            for (int row = 0; row < DIMENSION; row++) {
                b[row] = ecc.randomPoint(SECURE_RANDOM);
            }
            testGaussianElimination(matrixA, b);
        }
    }

    @Test
    public void testNotFullRankLinearSolver() {
        for (int round = 0; round < RANDOM_ROUND; round++) {
            // the first row is 0
            BigInteger[][] matrixA = new BigInteger[DIMENSION][DIMENSION];
            for (int column = 0; column < DIMENSION; column++) {
                matrixA[0][column] = BigInteger.ZERO;
            }
            // randomly choose remaining rows
            for (int row = 1; row < DIMENSION; row++) {
                for (int column = 0; column < DIMENSION; column++) {
                    matrixA[row][column] = BigIntegerUtils.randomNonNegative(n, SECURE_RANDOM);
                }
            }
            ECPoint[] b = new ECPoint[DIMENSION];
            // y_0 = 0
            b[0] = ecc.getInfinity();
            for (int row = 1; row < DIMENSION; row++) {
                b[row] = ecc.randomPoint(SECURE_RANDOM);
            }
            testGaussianElimination(matrixA, b);
        }
    }

    @Test
    public void testRectangularLinearSolver() {
        for (int round = 0; round < RANDOM_ROUND; round++) {
            BigInteger[][] matrixA = new BigInteger[DIMENSION][DIMENSION * 2];
            for (int row = 0; row < DIMENSION; row++) {
                for (int column = 0; column < DIMENSION * 2; column++) {
                    matrixA[row][column] = BigIntegerUtils.randomNonNegative(n, SECURE_RANDOM);
                }
            }
            ECPoint[] b = new ECPoint[DIMENSION];
            for (int row = 0; row < DIMENSION; row++) {
                b[row] = ecc.randomPoint(SECURE_RANDOM);
            }
            testGaussianElimination(matrixA, b);
        }
    }

    private void testGaussianElimination(BigInteger[][] matrixA, ECPoint[] b) {
        int ncol = matrixA[0].length;
        ECPoint[] x = new ECPoint[ncol];
        // free solve
        SystemInfo systemInfo = eccLinearSolver.freeSolve(matrixA, b, x);
        Assert.assertNotEquals(SystemInfo.Inconsistent, systemInfo);
        assertCorrect(matrixA, b, x);
        // full solve
        systemInfo = eccLinearSolver.fullSolve(matrixA, b, x);
        Assert.assertNotEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        assertCorrect(matrixA, b, x);
        ECPoint infinity = ecc.getInfinity();
        for (ECPoint xi : x) {
            Assert.assertNotEquals(infinity, xi);
        }
    }

    private void assertCorrect(BigInteger[][] matrixA, ECPoint[] b, ECPoint[] x) {
        int nRows = b.length;
        int nColumns = x.length;
        for (int iRow = 0; iRow < nRows; iRow++) {
            ECPoint res = ecc.getInfinity();
            for (int columnIndex = 0; columnIndex < nColumns; columnIndex++) {
                res = res.add(x[columnIndex].multiply(matrixA[iRow][columnIndex]));
            }
            Assert.assertEquals(b[iRow], res);
        }
    }
}
