package edu.alibaba.mpc4j.common.tool.lpn.matrix;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.common.tool.CommonConstants.BLOCK_BYTE_LENGTH;

/**
 * SparseMatrix 测试类，稀疏矩阵类生成、转换和代数操作测试。
 *
 * @author Hanwen Feng
 * @date 2022/03/03
 */
public class SparseMatrixTest {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SparseMatrixTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("000.000");
    /**
     * 默认测试行数
     */
    private final static int DEFAULT_ROWS = 100;
    /**
     * 默认测试列数
     */
    private final static int DEFAULT_COLS = 100;
    /**
     * 大规模测试行数
     */
    private final static int LARGE_ROWS = 10000;
    /**
     * 大规模测试列数
     */
    private final static int LARGE_COLS = 10000;
    /**
     * 每列Hamming重量
     */
    private final static int weight = 10;
    /**
     * 默认稀疏矩阵
     */
    private final SparseMatrix defaultMatrix;
    /**
     * 大规模稀疏矩阵
     */
    private final SparseMatrix largeMatrix;
    /**
     * 默认规模下三角矩阵
     */
    private final SparseMatrix defaultDiagMatrix;
    /**
     * 大规模下三角矩阵
     */
    private final SparseMatrix largeDiagMatrix;
    /**
     * 用于默认矩阵的左乘消息
     */
    private final byte[][] msgL;
    /**
     * 用于大规模矩阵的左乘消息
     */
    private final byte[][] largeMsgL;
    /**
     * 用于默认矩阵的右乘消息
     */
    private final byte[][] msgR;
    /**
     * 用于大规模矩阵的右乘消息
     */
    private final byte[][] largeMsgR;
    /**
     * boolean类型的消息
     */
    private final boolean[] boolMsg;

    /**
     * 构造函数
     */
    public SparseMatrixTest() {
        defaultMatrix = getRandomSparseMatrix(SparseMatrix.WorkType.Full, DEFAULT_ROWS, DEFAULT_COLS);
        defaultDiagMatrix = getRandomDiagMatrix(SparseMatrix.WorkType.Full, DEFAULT_ROWS);
        defaultDiagMatrix.setDiagMtx();

        largeMatrix = getRandomSparseMatrix(SparseMatrix.WorkType.Full, LARGE_ROWS, LARGE_COLS);
        largeDiagMatrix = getRandomDiagMatrix(SparseMatrix.WorkType.Full, LARGE_ROWS);
        largeDiagMatrix.setDiagMtx();

        msgR = getRandomBytes(DEFAULT_COLS);
        msgL = getRandomBytes(DEFAULT_ROWS);
        largeMsgL = getRandomBytes(LARGE_ROWS);
        largeMsgR = getRandomBytes(LARGE_COLS);
        boolMsg = getRandomBooleanArray();
    }

    /**
     * 测试稀疏矩阵构造方法的正确性
     */
    @Test
    public void testMatrixCreator() {
        assert defaultMatrix.isValid();
        assert defaultDiagMatrix.isValid();
        assert defaultDiagMatrix.isDiagMtx();

        // 补充测试循环矩阵的生成
        SparseMatrix cMtx = getRandomCyclicMatrix(false);
        SparseMatrix cMtx1 = getRandomCyclicMatrix(true);
        assert cMtx.isValid();
        assert cMtx1.isValid();
    }

    /**
     * 测试稀疏矩阵代数运算方法的正确性
     */
    @Test
    public void testOperations() {
        // 补充生成一些稀疏矩阵。
        SparseMatrix mtx2 = getRandomSparseMatrix(SparseMatrix.WorkType.Full, DEFAULT_ROWS, DEFAULT_COLS);
        SparseMatrix mtxCol = getRandomSparseMatrix(SparseMatrix.WorkType.ColsOnly, DEFAULT_ROWS, DEFAULT_COLS);
        SparseMatrix mtxRow = getRandomSparseMatrix(SparseMatrix.WorkType.RowsOnly, DEFAULT_ROWS, DEFAULT_COLS);

        // 测试稀疏矩阵和向量的乘法运算,包括左乘和右乘。
        Assert.assertArrayEquals(defaultMatrix.rmul(msgR), defaultMatrix.getDense().rmul(msgR));
        Assert.assertArrayEquals(defaultMatrix.lmul(msgL), defaultMatrix.getTransDense().rmul(msgL));
        Assert.assertArrayEquals(defaultMatrix.lmul(boolMsg), defaultMatrix.getTransDense().rmul(boolMsg));
        Assert.assertArrayEquals(defaultMatrix.lmul(msgL), defaultMatrix.getDense().lmul(msgL));
        Assert.assertArrayEquals(defaultMatrix.rmul(boolMsg), defaultMatrix.getDense().rmul(boolMsg));
        // 测试仅按行存储矩阵右乘向量，和仅按列存储矩阵左乘向量。
        Assert.assertArrayEquals(mtxRow.rmul(msgR), mtxRow.getDense().rmul(msgR));
        Assert.assertArrayEquals(mtxCol.lmul(msgL), mtxCol.getTransDense().rmul(msgL));

        // 补充生成一些下三角方阵。
        SparseMatrix dMtx2 = getRandomDiagMatrix(SparseMatrix.WorkType.RowsOnly, DEFAULT_ROWS);
        SparseMatrix dMtx3 = getRandomDiagMatrix(SparseMatrix.WorkType.ColsOnly, DEFAULT_ROWS);
        dMtx2.setDiagMtx();
        dMtx3.setDiagMtx();
        // 测试下三角方阵运算。
        Assert.assertArrayEquals(dMtx2.diagInvRmul(boolMsg), dMtx2.getDense().getInverse().rmul(boolMsg));
        Assert.assertArrayEquals(dMtx3.diagInvLmul(boolMsg), dMtx3.getTransDense().getInverse().rmul(boolMsg));
        Assert.assertArrayEquals(dMtx2.diagInvRmul(msgL), dMtx2.getDense().getInverse().rmul(msgL));
        Assert.assertArrayEquals(dMtx3.diagInvLmul(msgL), dMtx3.getTransDense().getInverse().rmul(msgL));

        // 测试稀疏矩阵和稀疏矩阵相乘,稀疏矩阵和DenseMatrix相乘。
        Assert.assertEquals(defaultMatrix.getDense().multi(mtx2.getDense()),
                defaultMatrix.multi(mtx2, SparseMatrix.WorkType.Full).getDense());
        Assert.assertEquals(defaultMatrix.getDense().multi(mtx2.getDense()), defaultMatrix.rmul(mtx2.getDense()));
        Assert.assertEquals(mtx2.getTransDense().multi(defaultMatrix.getDense()), mtx2.lmul(defaultMatrix.getDense()));

        // 测试稀疏矩阵和稀疏矩阵相加。
        Assert.assertEquals(defaultMatrix.getDense().add(mtx2.getDense()),
                defaultMatrix.add(mtx2, SparseMatrix.WorkType.Full).getDense());

        // 测试下三角矩阵和DenseMatrix的乘法。
        Assert.assertEquals(defaultDiagMatrix.getDense().getInverse().multi(defaultMatrix.getDense()),
                defaultDiagMatrix.diagInvRmul(defaultMatrix.getDense()));
        Assert.assertEquals(dMtx2.getDense().getInverse().multi(defaultMatrix.getDense()),
                dMtx2.diagInvRmul(defaultMatrix.getDense()));
        Assert.assertEquals(dMtx3.getTransDense().getInverse().multi(defaultMatrix.getDense()),
                dMtx3.diagInvLmul(defaultMatrix.getDense()));
    }

    /**
     * 测试默认规模矩阵运算的效率
     */
    @Test
    public void testDefaultEfficiency() {
        defaultMatrix.setParallel(false);
        defaultDiagMatrix.setParallel(false);
        testMultiEfficiency(defaultMatrix, msgL, msgR);
        testDiagInvEfficiency(defaultDiagMatrix, msgL);
    }

    /**
     * 测试大规模矩阵运算的效率
     */
    @Test
    public void testLargeEfficiency() {
        largeMatrix.setParallel(false);
        largeDiagMatrix.setParallel(false);
        testMultiEfficiency(largeMatrix, largeMsgL, largeMsgR);
        testDiagInvEfficiency(largeDiagMatrix, largeMsgL);
    }

    /**
     * 测试默认规模矩阵并行运算的效率
     */
    @Test
    public void testDefaultParallelEfficiency() {
        defaultMatrix.setParallel(true);
        defaultDiagMatrix.setParallel(true);
        testMultiEfficiency(defaultMatrix, msgL, msgR);
        testDiagInvEfficiency(defaultDiagMatrix, msgL);
    }

    /**
     * 测试大规模矩阵并行运算的效率
     */
    @Test
    public void testLargeParallelEfficiency() {
        largeMatrix.setParallel(true);
        largeDiagMatrix.setParallel(true);
        testMultiEfficiency(largeMatrix, largeMsgL, largeMsgR);
        testDiagInvEfficiency(largeDiagMatrix, largeMsgL);
    }

    private void testMultiEfficiency(SparseMatrix mtx, byte[][] msgL, byte[][] msgR) {
        // 预热。
        mtx.lmul(msgL);
        StopWatch stopwatch = new StopWatch();
        // 测试sparseMtx右乘。
        stopwatch.start();
        mtx.rmul(msgR);
        stopwatch.stop();
        double sparseMatrixRmulTime = (double) stopwatch.getTime(TimeUnit.MICROSECONDS) / 1000;
        stopwatch.reset();
        // 测试sparseMtx左乘。
        stopwatch.start();
        mtx.lmul(msgL);
        stopwatch.stop();
        double sparseMatrixLmulTime = (double) stopwatch.getTime(TimeUnit.MICROSECONDS) / 1000;
        stopwatch.reset();

        LOGGER.info("Operation           \tTime(ms)");
        LOGGER.info("SparseMatrix RightMulti.\t{}", TIME_DECIMAL_FORMAT.format(sparseMatrixRmulTime));
        LOGGER.info("SparseMatrix LeftMulti.\t{}", TIME_DECIMAL_FORMAT.format(sparseMatrixLmulTime));

    }

    private void testDiagInvEfficiency(SparseMatrix dmtx, byte[][] msgL) {
        // 预热。
        dmtx.lmul(msgL);
        StopWatch stopwatch = new StopWatch();
        // 测试SparseMtx 下三角矩阵右乘。
        stopwatch.start();
        dmtx.diagInvRmul(msgL);
        stopwatch.stop();
        double sparseMatrixDiagInvRmulTime = (double) stopwatch.getTime(TimeUnit.MICROSECONDS) / 1000;
        stopwatch.reset();
        // 测试SparseMtx 下三角矩阵左乘。
        stopwatch.start();
        dmtx.diagInvLmul(msgL);
        stopwatch.stop();
        double sparseMatrixDiagInvLmulTime = (double) stopwatch.getTime(TimeUnit.MICROSECONDS) / 1000;
        stopwatch.reset();

        LOGGER.info("Operation           \tTime(ms)");
        LOGGER.info("SparseMatrix DiagInvert RightMulti.\t{}", TIME_DECIMAL_FORMAT.format(sparseMatrixDiagInvRmulTime));
        LOGGER.info("SparseMatrix DiagInvert LeftMulti.\t{}", TIME_DECIMAL_FORMAT.format(sparseMatrixDiagInvLmulTime));
    }

    private static SparseMatrix getRandomSparseMatrix(SparseMatrix.WorkType type, int rows, int cols) {
        SparseMatrix.PointList pnts = new SparseMatrix.PointList(rows, cols);
        int pntNumber = rows * weight;
        HashSet<SparseMatrix.Point> pointSet = new HashSet<>();
        for (int n = 0; n < pntNumber; n++) {
            int i = SECURE_RANDOM.nextInt(rows);
            int j = SECURE_RANDOM.nextInt(cols);
            SparseMatrix.Point pRandom = new SparseMatrix.Point(i, j);
            if (!pointSet.contains(pRandom)) {
                pnts.addPoint(pRandom);
                pointSet.add(pRandom);
            }
        }
        return SparseMatrix.createFromPointsList(pnts, type);
    }

    private SparseMatrix getRandomCyclicMatrix(boolean asRow) {
        int[] initArray = new int[weight];
        HashSet<Integer> hashSet = new HashSet<>();
        int bitArrayLength = asRow ? SparseMatrixTest.DEFAULT_COLS : SparseMatrixTest.DEFAULT_ROWS;
        for (int i = 0; i < weight; ) {
            int index = SECURE_RANDOM.nextInt(bitArrayLength);
            if (!hashSet.contains(index)) {
                initArray[i] = index;
                hashSet.add(index);
                i++;
            }
        }
        Arrays.sort(initArray);
        return asRow ? SparseMatrix.createFromCyclicCol(SparseMatrixTest.DEFAULT_ROWS,
                    SparseMatrixTest.DEFAULT_COLS, initArray, SparseMatrix.WorkType.Full) :
                SparseMatrix.createFromCyclicRow(SparseMatrixTest.DEFAULT_ROWS,
                        SparseMatrixTest.DEFAULT_COLS, initArray, SparseMatrix.WorkType.Full);
    }

    private static SparseMatrix getRandomDiagMatrix(SparseMatrix.WorkType type, int rows) {
        SparseMatrix.PointList pnts = new SparseMatrix.PointList(rows, rows);
        int pntNumber = rows * weight;
        HashSet<SparseMatrix.Point> pointSet = new HashSet<>();
        // 添加对角线。
        for (int n = 0; n < rows; n++) {
            SparseMatrix.Point p = new SparseMatrix.Point(n, n);
            pnts.addPoint(p);
            pointSet.add(p);
        }
        // 在下三角区域添加随机点。
        for (int n = 0; n < pntNumber; n++) {
            int i = SECURE_RANDOM.nextInt(rows);
            int j = SECURE_RANDOM.nextInt(i + 1);
            SparseMatrix.Point pRandom = new SparseMatrix.Point(i, j);
            if (!pointSet.contains(pRandom)) {
                pnts.addPoint(pRandom);
                pointSet.add(pRandom);
            }
        }
        return SparseMatrix.createFromPointsList(pnts, type);
    }

    private static boolean[] getRandomBooleanArray() {
        boolean[] booleans = new boolean[SparseMatrixTest.DEFAULT_ROWS];
        for (int i = 0; i < SparseMatrixTest.DEFAULT_ROWS; i++) {
            booleans[i] = SECURE_RANDOM.nextBoolean();
        }
        return booleans;
    }

    private byte[][] getRandomBytes(int length) {
        byte[][] bytes = new byte[length][BLOCK_BYTE_LENGTH];
        for (int i = 0; i < length; i++) {
            SECURE_RANDOM.nextBytes(bytes[i]);
        }
        return bytes;
    }
}
