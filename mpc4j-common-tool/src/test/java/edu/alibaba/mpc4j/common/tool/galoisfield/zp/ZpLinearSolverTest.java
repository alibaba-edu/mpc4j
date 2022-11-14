package edu.alibaba.mpc4j.common.tool.galoisfield.zp;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Zp高斯消元法测试。
 *
 * @author Weiran Liu
 * @date 2021/05/08
 */
public class ZpLinearSolverTest {
    /**
     * 随机测试轮数
     */
    private static final int RANDOM_ROUND = 1000;
    /**
     * 矩阵维度，取奇数以验证所有可能的边界情况
     */
    private static final int DIMENSION = 7;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * Zp有限域
     */
    private final Zp zp;
    /**
     * 线性求解器
     */
    private final ZpLinearSolver zpLinearSolver;

    public ZpLinearSolverTest() {
        BigInteger p = ZpManager.getPrime(CommonConstants.BLOCK_BIT_LENGTH);
        zp = ZpFactory.createInstance(EnvType.STANDARD, p);
        zpLinearSolver = new ZpLinearSolver(zp);
    }

    @Test
    public void testFullRankLinearSolver() {
        for (int round = 0; round < RANDOM_ROUND; round++) {
            // 当矩阵都是随机点时，有非常高的概率有解
            BigInteger[][] matrixA = new BigInteger[DIMENSION][DIMENSION];
            for (int row = 0; row < DIMENSION; row++) {
                for (int column = 0; column < DIMENSION; column++) {
                    matrixA[row][column] = zp.createRandom(SECURE_RANDOM);
                }
            }
            BigInteger[] b = new BigInteger[DIMENSION];
            for (int row = 0; row < DIMENSION; row++) {
                b[row] = zp.createRandom(SECURE_RANDOM);
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
                    matrixA[row][column] = zp.createRandom(SECURE_RANDOM);
                }
            }
            BigInteger[] b = new BigInteger[DIMENSION];
            // y_0设置为0
            b[0] = BigInteger.ZERO;
            for (int row = 1; row < DIMENSION; row++) {
                b[row] = zp.createRandom(SECURE_RANDOM);
            }
            testGaussianElimination(matrixA, b);
        }
    }

    @Test
    public void testRectangularLinearSolver() {
        for (int round = 0; round < RANDOM_ROUND; round++) {
            // 当矩阵都是随机点时，有非常高的概率有解
            BigInteger[][] matrixA = new BigInteger[DIMENSION][DIMENSION * 2];
            for (int row = 0; row < DIMENSION; row++) {
                for (int column = 0; column < DIMENSION * 2; column++) {
                    matrixA[row][column] = zp.createRandom(SECURE_RANDOM);
                }
            }
            BigInteger[] b = new BigInteger[DIMENSION];
            for (int row = 0; row < DIMENSION; row++) {
                b[row] = zp.createRandom(SECURE_RANDOM);
            }
            testGaussianElimination(matrixA, b);
        }
    }

    private void testGaussianElimination(BigInteger[][] matrixA, BigInteger[] b) {
        int nrow = b.length;
        int ncol = matrixA[0].length;
        BigInteger[] x = new BigInteger[ncol];
        SystemInfo systemInfo = zpLinearSolver.solve(matrixA, b, x, true);
        Assert.assertNotEquals(SystemInfo.Inconsistent, systemInfo);
        for (int rowIndex = 0; rowIndex < nrow; rowIndex++) {
            BigInteger res = BigInteger.ZERO;
            for (int columnIndex = 0; columnIndex < ncol; columnIndex++) {
                res = zp.add(res, zp.mul(x[columnIndex], matrixA[rowIndex][columnIndex]));
            }
            Assert.assertEquals(b[rowIndex], res);
        }
    }
}
