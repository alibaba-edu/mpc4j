package edu.alibaba.mpc4j.common.tool.lpn.matrix;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.common.tool.CommonConstants.BLOCK_BYTE_LENGTH;


/**
 * ExtremeSparseMatrix测试类，同时测试了ExtremeSparseMatrix和CyclicSparseMatrix。
 * 首先使用一个regular的sparseVector作为初始向量，生成循环的稀疏矩阵。
 * 然后对循环进行切割，得到A和D两个子矩阵。矩阵A和CylicSparseMatrix表示同一矩阵，矩阵D和ExtremeSparseMatrix表示同一矩阵。
 * 分别测试两类特殊矩阵代数操作的正确性。
 *
 * @author Hanwen Feng
 * @date 2022/03/14
 */
public class ExtremeSparseMatrixTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtremeSparseMatrixTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("000.000");
    /**
     * 稀疏矩阵的行数
     */
    private final int rows = 10000;
    /**
     * 稀疏矩阵的列数
     */
    private final int cols = 10000;
    /**
     * 将稀疏矩阵切割为CyclicSparseMatrix和ExtremeSparseMatrix的分割点。
     */
    private final int splitIndex = 20;
    /**
     * 随机消息1
     */
    private final byte[][] randomMsg1;
    /**
     * 随机消息2
     */
    private final byte[][] randomMsg2;
    /**
     * 稀疏矩阵A
     */
    private SparseMatrix matrixA;
    /**
     * 稀疏矩阵D
     */
    private SparseMatrix matrixD;
    /**
     * 循环矩阵 CyclicSparseMatrix
     */
    private CyclicSparseMatrix cyclicSparseMatrixA;
    /**
     * 极度稀疏矩阵 ExtremeSparseMatrix
     */
    private ExtremeSparseMatrix xMatrixD;
    /**
     * 构造函数
     */
    public ExtremeSparseMatrixTest() {
        init();
        randomMsg1 = new byte[rows - splitIndex][BLOCK_BYTE_LENGTH];
        randomMsg2 = new byte[splitIndex][BLOCK_BYTE_LENGTH];

        for (byte[] bytes : randomMsg1) {
            SECURE_RANDOM.nextBytes(bytes);
        }
        for (byte[] bytes : randomMsg2) {
            SECURE_RANDOM.nextBytes(bytes);
        }
    }

    /**
     * 测试CyclicSparseMatrix运算的正确性和效率
     */
    @Test
    public void testCyclicEfficiency() {
        StopWatch stopwatch = new StopWatch();

        byte[][] msgY = new byte[cols][BLOCK_BYTE_LENGTH];
        byte[][] msgY1 = new byte[cols][BLOCK_BYTE_LENGTH];
        // 预热。
        matrixA.lmul(randomMsg1);

        // 测试SparseMatrix乘法时间。
        stopwatch.start();
        matrixA.lmulAdd(randomMsg1, msgY);
        stopwatch.stop();
        double originTime = (double) stopwatch.getTime(TimeUnit.MICROSECONDS) / 1000;
        stopwatch.reset();
        // 测试CyclicSparseMatrix乘法时间。
        stopwatch.start();
        cyclicSparseMatrixA.lmulAdd(randomMsg1, msgY1);
        stopwatch.stop();
        double cyclicTime = (double) stopwatch.getTime(TimeUnit.MICROSECONDS) / 1000;
        stopwatch.reset();
        // 验证SparseMatrix提供的运算和CyclicSparseMatrix提供的运算结果相同。
        Assert.assertArrayEquals(msgY, msgY1);
        // 测试SparseMatrix并发时间。
        matrixA.setParallel(true);
        stopwatch.start();
        matrixA.lmulAdd(randomMsg1, msgY);
        stopwatch.stop();
        double originParaTime = (double) stopwatch.getTime(TimeUnit.MICROSECONDS) / 1000;
        stopwatch.reset();

        LOGGER.info("matrix Type         \tTime(ms)");
        LOGGER.info("origin SparseMatrix time\t{}", TIME_DECIMAL_FORMAT.format(originTime));
        LOGGER.info("new CyclicMatrix time.\t{}", TIME_DECIMAL_FORMAT.format(cyclicTime));
        LOGGER.info("origin sparse Matrix parallel time.\t{}", TIME_DECIMAL_FORMAT.format(originParaTime));
    }

    /**
     * ExtremeSparseMatrix运算的正确性和效率
     */
    @Test
    public void testExtremeSparseEfficiency() {
        StopWatch stopwatch = new StopWatch();
        byte[][] msgY = new byte[cols][BLOCK_BYTE_LENGTH];
        byte[][] msgY1 = new byte[cols][BLOCK_BYTE_LENGTH];
        // 预热。
        matrixD.lmul(randomMsg2);
        // 测试SparseMatrix乘法时间。
        stopwatch.start();
        matrixD.lmulAdd(randomMsg2, msgY);
        stopwatch.stop();
        double originTime = (double) stopwatch.getTime(TimeUnit.MICROSECONDS) / 1000;
        stopwatch.reset();
        // 测试CyclicSparseMatrix乘法时间。
        stopwatch.start();
        xMatrixD.lmulAdd(randomMsg2, msgY1);
        stopwatch.stop();
        double cyclicTime = (double) stopwatch.getTime(TimeUnit.MICROSECONDS) / 1000;
        stopwatch.reset();

        Assert.assertArrayEquals(msgY, msgY1);

        LOGGER.info("matrix Type         \tTime(ms)");
        LOGGER.info("origin SparseMatrix time\t{}", TIME_DECIMAL_FORMAT.format(originTime));
        LOGGER.info("Extremely SparseMatrix time.\t{}", TIME_DECIMAL_FORMAT.format(cyclicTime));
    }

    private void init() {
        // 生成regular的稀疏随机数。
        int weight = 10;
        int[] initVector = new int[weight];
        int randomIndex = SECURE_RANDOM.nextInt(cols / weight);
        for (int i = 0; i < weight; i++) {
            initVector[i] = randomIndex + i * cols / weight;
        }

        SparseMatrix fullMatrix = SparseMatrix.createFromCyclicCol(rows, cols, initVector, SparseMatrix.WorkType.ColsOnly);
        cyclicSparseMatrixA = new CyclicSparseMatrix(initVector, rows, rows - splitIndex, cols);
        matrixA = fullMatrix.getSubMatrix(0, 0, rows - splitIndex, cols, SparseMatrix.WorkType.ColsOnly);
        matrixD = fullMatrix.getSubMatrix(rows - splitIndex, 0, splitIndex, cols, SparseMatrix.WorkType.ColsOnly);
        xMatrixD = matrixD.getExtremeSparseMatrix();
    }
}
