package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * 椭圆曲线高斯消元法测试。
 *
 * @author Weiran Liu
 * @date 2021/05/08
 */
public class EccLinearSolverTest {
    /**
     * 随机测试轮数
     */
    private static final int RANDOM_ROUND = 200;
    /**
     * 矩阵维度，取奇数以验证所有可能的边界情况
     */
    private static final int DIMENSION = 7;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * 椭圆曲线指数模数
     */
    private final BigInteger n;
    /**
     * 线性求解器
     */
    private final EccLinearSolver eccLinearSolver;

    public EccLinearSolverTest() {
        ecc = EccFactory.createInstance(EnvType.STANDARD);
        n = ecc.getN();
        eccLinearSolver = new EccLinearSolver(ecc);
    }

    @Test
    public void testFullRankLinearSolver() {
        for (int round = 0; round < RANDOM_ROUND; round++) {
            // 当矩阵都是随机点时，有非常高的概率有解
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
            // 将第一行设置为全0
            BigInteger[][] matrixA = new BigInteger[DIMENSION][DIMENSION];
            for (int column = 0; column < DIMENSION; column++) {
                matrixA[0][column] = BigInteger.ZERO;
            }
            // 剩余行随机选择
            for (int row = 1; row < DIMENSION; row++) {
                for (int column = 0; column < DIMENSION; column++) {
                    matrixA[row][column] = BigIntegerUtils.randomNonNegative(n, SECURE_RANDOM);
                }
            }
            ECPoint[] b = new ECPoint[DIMENSION];
            // y_0设置为0
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
        int nrow = b.length;
        int ncol = matrixA[0].length;
        ECPoint[] x = new ECPoint[ncol];
        SystemInfo systemInfo = eccLinearSolver.solve(matrixA, b, x, true);
        Assert.assertNotEquals(SystemInfo.Inconsistent, systemInfo);
        for (int rowIndex = 0; rowIndex < nrow; rowIndex++) {
            ECPoint res = ecc.getInfinity();
            for (int columnIndex = 0; columnIndex < ncol; columnIndex++) {
                res = res.add(x[columnIndex].multiply(matrixA[rowIndex][columnIndex]));
            }
            Assert.assertEquals(b[rowIndex], res);
        }
    }
}
