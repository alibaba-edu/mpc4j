package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import cc.redberry.rings.linear.LinearSolver;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * GF2E高斯消元法测试。
 *
 * @author Weiran Liu
 * @date 2022/7/6
 */
public class Gf2eLinearSolverTest {
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
     * GF2E
     */
    private final Gf2e gf2e;
    /**
     * 线性求解器
     */
    private final Gf2eLinearSolver gf2eLinearSolver;

    public Gf2eLinearSolverTest() {
        gf2e = Gf2eFactory.createInstance(EnvType.STANDARD, CommonConstants.STATS_BIT_LENGTH);
        gf2eLinearSolver = new Gf2eLinearSolver(gf2e);
    }

    @Test
    public void testFullRankLinearSolver() {
        for (int round = 0; round < RANDOM_ROUND; round++) {
            // 当矩阵都是随机点时，有非常高的概率有解
            byte[][][] matrixA = new byte[DIMENSION][DIMENSION][];
            for (int row = 0; row < DIMENSION; row++) {
                for (int column = 0; column < DIMENSION; column++) {
                    matrixA[row][column] = gf2e.createRandom(SECURE_RANDOM);
                }
            }
            byte[][] b = new byte[DIMENSION][];
            for (int row = 0; row < DIMENSION; row++) {
                b[row] = gf2e.createRandom(SECURE_RANDOM);
            }
            testGaussianElimination(matrixA, b);
        }
    }

    @Test
    public void testNotFullRankLinearSolver() {
        for (int round = 0; round < RANDOM_ROUND; round++) {
            // 将第一行设置为全0
            byte[][][] matrixA = new byte[DIMENSION][DIMENSION][];
            for (int column = 0; column < DIMENSION; column++) {
                matrixA[0][column] = gf2e.createZero();
            }
            // 剩余行随机选择
            for (int row = 1; row < DIMENSION; row++) {
                for (int column = 0; column < DIMENSION; column++) {
                    matrixA[row][column] = gf2e.createRandom(SECURE_RANDOM);
                }
            }
            byte[][] b = new byte[DIMENSION][];
            // y_0设置为0
            b[0] = gf2e.createZero();
            for (int row = 1; row < DIMENSION; row++) {
                b[row] = gf2e.createRandom(SECURE_RANDOM);
            }
            testGaussianElimination(matrixA, b);
        }
    }

    @Test
    public void testRectangularLinearSolver() {
        for (int round = 0; round < RANDOM_ROUND; round++) {
            byte[][][] matrixA = new byte[DIMENSION][DIMENSION * 2][];
            for (int row = 0; row < DIMENSION; row++) {
                for (int column = 0; column < DIMENSION * 2; column++) {
                    matrixA[row][column] = gf2e.createRandom(SECURE_RANDOM);
                }
            }
            byte[][] b = new byte[DIMENSION][];
            for (int row = 0; row < DIMENSION; row++) {
                b[row] = gf2e.createRandom(SECURE_RANDOM);
            }
            testGaussianElimination(matrixA, b);
        }
    }

    private void testGaussianElimination(byte[][][] matrixA, byte[][] b) {
        int nrow = b.length;
        int ncol = matrixA[0].length;
        // byte[]是可变的，因此要复制一份出来
        byte[][][] copyMatrixA = new byte[nrow][ncol][];
        for (int row = 0; row < nrow; row++) {
            copyMatrixA[row] = BytesUtils.clone(matrixA[row]);
        }
        byte[][] copyB = BytesUtils.clone(b);
        // 求解x
        byte[][] x = new byte[ncol][];
        LinearSolver.SystemInfo systemInfo = gf2eLinearSolver.solve(matrixA, b, x, true);
        Assert.assertNotEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        for (int rowIndex = 0; rowIndex < nrow; rowIndex++) {
            byte[] res = gf2e.createZero();
            for (int columnIndex = 0; columnIndex < ncol; columnIndex++) {
                gf2e.addi(res, gf2e.mul(x[columnIndex], copyMatrixA[rowIndex][columnIndex]));
            }
            Assert.assertArrayEquals(copyB[rowIndex], res);
        }
    }
}
