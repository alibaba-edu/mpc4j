package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrixTestUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * 稀疏矩阵测试。
 *
 * @author Hanwen Feng
 * @date 2022/10/09
 */
public class SparseBitMatrixTest {
    /**
     * 测试维度
     */
    private static final int[] SIZES = new int[]{40, 100, 200, 400};
    /**
     * 测试汉明重量
     */
    private static final int[] WEIGHTS = new int[]{5, 8, 16};
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 随机测试轮数
     */
    private static final int ROUND = 40;

    @Test
    public void testRandomAdd() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                for (int weight : WEIGHTS)
                    testRandomAdd(rows, columns, weight);
            }
        }
    }

    private void testRandomAdd(int rows, int columns, int weight) {
        // 生成随机矩阵
        SparseBitMatrix sparseBitMatrix0 = SparseBitMatrixTestUtils.createRandom(columns, rows, weight, SECURE_RANDOM);
        SparseBitMatrix sparseBitMatrix1 = SparseBitMatrixTestUtils.createRandom(columns, rows, weight, SECURE_RANDOM);
        SparseBitMatrix sumSparseMatrix = sparseBitMatrix0.add(sparseBitMatrix1);
        // 转换为稠密矩阵
        DenseBitMatrix denseBitMatrix0 = sparseBitMatrix0.toTransDenseBitMatrix().transpose(EnvType.STANDARD_JDK, false);
        DenseBitMatrix denseBitMatrix1 = sparseBitMatrix1.toTransDenseBitMatrix().transpose(EnvType.STANDARD_JDK, false);
        DenseBitMatrix sumDenseMatrix = denseBitMatrix0.add(denseBitMatrix1);
        // 验证
        Assert.assertEquals(sumSparseMatrix.toTransDenseBitMatrix().transpose(EnvType.STANDARD_JDK, false), sumDenseMatrix);
    }

    @Test
    public void testTransMultiply() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                for (int weight : WEIGHTS)
                    testTransMultiply(rows, columns, weight);
            }
        }
    }

    private void testTransMultiply(int rows, int columns, int weight) {
        // 生成随机矩阵
        SparseBitMatrix sparseBitMatrix0 = SparseBitMatrixTestUtils.createRandom(columns, rows, weight, SECURE_RANDOM);
        // 转换为稠密矩阵
        DenseBitMatrix denseBitMatrix0 = sparseBitMatrix0.toTransDenseBitMatrix();
        // 生成稠密矩阵
        DenseBitMatrix denseBitMatrix1 = DenseBitMatrixTestUtils.createRandom(rows, columns, SECURE_RANDOM);
        // 验证
        Assert.assertEquals(sparseBitMatrix0.transMultiply(denseBitMatrix1), denseBitMatrix0.multiply(denseBitMatrix1));
    }

    @Test
    public void testTranspose() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                for (int weight : WEIGHTS)
                    testTranspose(rows, columns, weight);
            }
        }
    }

    private void testTranspose(int rows, int columns, int weight) {
        for (int round = 0; round < ROUND; round++) {
            SparseBitMatrix origin = SparseBitMatrixTestUtils.createRandom(rows, columns, weight, SECURE_RANDOM);
            SparseBitMatrix transpose = origin.transpose();
            SparseBitMatrix recover = transpose.transpose();
            Assert.assertEquals(origin, recover);
        }
    }

    @Test
    public void testLmul() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                for (int weight : WEIGHTS)
                    testLmul(rows, columns, weight);
            }
        }
    }

    private void testLmul(int rows, int columns, int weight) {
        for (int round = 0; round < ROUND; round++) {
            boolean[] v = SparseBitMatrixTestUtils.generateRandomBitVector(rows, SECURE_RANDOM);
            SparseBitMatrix sparseBitMatrix = SparseBitMatrixTestUtils.createRandom(columns, rows, weight, SECURE_RANDOM);
            DenseBitMatrix denseBitMatrix = sparseBitMatrix.toTransDenseBitMatrix().transpose(EnvType.STANDARD_JDK, false);
            Assert.assertArrayEquals(sparseBitMatrix.lmul(v), denseBitMatrix.lmul(v));
        }
    }

    @Test
    public void testLmulAddi() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                for (int weight : WEIGHTS)
                    testLmulAddi(rows, columns, weight);
            }
        }
    }

    private void testLmulAddi(int rows, int columns, int weight) {
        for (int round = 0; round < ROUND; round++) {
            boolean[] v = SparseBitMatrixTestUtils.generateRandomBitVector(rows, SECURE_RANDOM);
            boolean[] t0 = SparseBitMatrixTestUtils.generateRandomBitVector(columns, SECURE_RANDOM);
            boolean[] t1 = Arrays.copyOf(t0, t0.length);
            SparseBitMatrix sparseBitMatrix = SparseBitMatrixTestUtils.createRandom(columns, rows, weight, SECURE_RANDOM);
            DenseBitMatrix denseBitMatrix = sparseBitMatrix.toTransDenseBitMatrix().transpose(EnvType.STANDARD_JDK, false);
            sparseBitMatrix.lmulAddi(v, t0);
            denseBitMatrix.lmulAddi(v, t1);
            Assert.assertArrayEquals(t0, t1);
        }
    }

    @Test
    public void testLextMul() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                for (int weight : WEIGHTS)
                    testLextMul(rows, columns, weight);
            }
        }
    }

    private void testLextMul(int rows, int columns, int weight) {
        for (int round = 0; round < ROUND; round++) {
            byte[][] v = SparseBitMatrixTestUtils.generateRandomExtendFieldVector(rows, CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
            SparseBitMatrix sparseBitMatrix = SparseBitMatrixTestUtils.createRandom(columns, rows, weight, SECURE_RANDOM);
            DenseBitMatrix denseBitMatrix = sparseBitMatrix.toTransDenseBitMatrix().transpose(EnvType.STANDARD_JDK, false);
            Assert.assertArrayEquals(sparseBitMatrix.lExtMul(v), denseBitMatrix.lExtMul(v));
        }
    }

    @Test
    public void testLextMulAddi() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                for (int weight : WEIGHTS)
                    testLextMulAddi(rows, columns, weight);
            }
        }
    }

    private void testLextMulAddi(int rows, int columns, int weight) {
        for (int round = 0; round < ROUND; round++) {
            byte[][] v = SparseBitMatrixTestUtils.generateRandomExtendFieldVector(rows, CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
            byte[][] t0 = SparseBitMatrixTestUtils.generateRandomExtendFieldVector(columns, CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
            byte[][] t1 = Arrays.stream(t0).map(BytesUtils::clone).toArray(byte[][]::new);
            SparseBitMatrix sparseBitMatrix = SparseBitMatrixTestUtils.createRandom(columns, rows, weight, SECURE_RANDOM);
            DenseBitMatrix denseBitMatrix = sparseBitMatrix.toTransDenseBitMatrix().transpose(EnvType.STANDARD_JDK, false);
            sparseBitMatrix.lExtMulAddi(v, t0);
            denseBitMatrix.lExtMulAddi(v, t1);
            Assert.assertArrayEquals(t0, t1);
        }
    }

    @Test
    public void testSubMatrix() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                for (int weight : WEIGHTS)
                    testSubMatrix(rows, columns, weight);
            }
        }
    }

    private void testSubMatrix(int rows, int columns, int weight) {
        SparseBitMatrix whole = SparseBitMatrixTestUtils.createRandom(columns, rows, weight, SECURE_RANDOM);

        int splitIndex = rows / 2;
        SparseBitMatrix subA = whole.getSubMatrix(0, columns, 0, splitIndex);
        SparseBitMatrix subB = whole.getSubMatrix(0, columns, splitIndex, rows);

        boolean[] v = SparseBitMatrixTestUtils.generateRandomBitVector(rows, SECURE_RANDOM);

        boolean[] subV0 = Arrays.copyOfRange(v, 0, splitIndex);
        boolean[] subV1 = Arrays.copyOfRange(v, splitIndex, rows);
        boolean[] output0 = subA.lmul(subV0);
        boolean[] output1 = subB.lmul(subV1);
        for (int i = 0; i < output0.length; i++) {
            output0[i] ^= output1[i];
        }
        Assert.assertArrayEquals(whole.lmul(v), output0);
    }

    @Test
    public void testTriangularMatrix() {
        for (int size : SIZES) {
            for (int weight : WEIGHTS)
                testTriangularMatrix(size, weight);
        }
    }

    private void testTriangularMatrix(int size, int weight) {
        LowerTriangularSparseBitMatrix lowerMatrix = SparseBitMatrixTestUtils.createRandomLowerTriangular(size, weight, SECURE_RANDOM);
        boolean[] input = SparseBitMatrixTestUtils.generateRandomBitVector(size, SECURE_RANDOM);
        boolean[] output0 = lowerMatrix.lmul(input);
        boolean[] recoveredInput0 = lowerMatrix.invLmul(output0);
        Assert.assertArrayEquals(input, recoveredInput0);

        UpperTriangularSparseBitMatrix upperMatrix = lowerMatrix.transpose();
        boolean[] output1 = upperMatrix.lmul(input);
        boolean[] recoveredInput1 = upperMatrix.invLmul(output1);
        Assert.assertArrayEquals(input, recoveredInput1);
    }

    @Test
    public void testExtremeSparseMatrix() {
        for (int rows : SIZES) {
            for (int cols : SIZES) {
                for (int weight : WEIGHTS)
                    testExtremeSparseMatrix(rows, cols, weight);
            }
        }
    }

    private void testExtremeSparseMatrix(int rows, int cols, int weight) {
        SparseBitMatrix sparseBitMatrix = SparseBitMatrixTestUtils.createRandom(cols, rows, weight, SECURE_RANDOM);
        ExtremeSparseBitMatrix extremeSparseBitMatrix = sparseBitMatrix.toExtremeSparseMatrix();
        boolean[] input = SparseBitMatrixTestUtils.generateRandomBitVector(rows, SECURE_RANDOM);
        byte[][] extInput = SparseBitMatrixTestUtils.generateRandomExtendFieldVector(rows, CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
        Assert.assertArrayEquals(sparseBitMatrix.lmul(input), extremeSparseBitMatrix.lmul(input));
        Assert.assertArrayEquals(sparseBitMatrix.lExtMul(extInput), extremeSparseBitMatrix.lExtMul(extInput));
    }

}
