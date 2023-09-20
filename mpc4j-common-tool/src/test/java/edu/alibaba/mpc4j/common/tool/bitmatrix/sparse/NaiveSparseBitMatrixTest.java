package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.ByteDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
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
public class NaiveSparseBitMatrixTest {
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
        NaiveSparseBitMatrix sparseBitMatrix0 = NaiveSparseBitMatrix.createRandom(rows, columns, weight, SECURE_RANDOM);
        NaiveSparseBitMatrix sparseBitMatrix1 = NaiveSparseBitMatrix.createRandom(rows, columns, weight, SECURE_RANDOM);
        NaiveSparseBitMatrix xorSparseBitMatrix = sparseBitMatrix0.xor(sparseBitMatrix1);
        // 转换为稠密矩阵
        DenseBitMatrix denseBitMatrix0 = sparseBitMatrix0.toDense();
        DenseBitMatrix denseBitMatrix1 = sparseBitMatrix1.toDense();
        DenseBitMatrix xorDenseBitMatrix = denseBitMatrix0.xor(denseBitMatrix1);
        // 验证
        Assert.assertEquals(xorSparseBitMatrix.toDense(), xorDenseBitMatrix);
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
        NaiveSparseBitMatrix naiveSparseBitMatrix0 = NaiveSparseBitMatrix.createRandom(rows, columns, weight, SECURE_RANDOM);
        // 转换为稠密矩阵
        DenseBitMatrix denseBitMatrix0 = naiveSparseBitMatrix0.transposeDense();
        // 生成稠密矩阵
        DenseBitMatrix denseBitMatrix1 = ByteDenseBitMatrix.createRandom(rows, columns, SECURE_RANDOM);
        // 验证
        Assert.assertEquals(naiveSparseBitMatrix0.transposeMultiply(denseBitMatrix1), denseBitMatrix0.multiply(denseBitMatrix1));
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
            NaiveSparseBitMatrix origin = NaiveSparseBitMatrix.createRandom(rows, columns, weight, SECURE_RANDOM);
            NaiveSparseBitMatrix transpose = origin.transpose();
            NaiveSparseBitMatrix recover = transpose.transpose();
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
            boolean[] v = BinaryUtils.randomBinary(rows, SECURE_RANDOM);
            NaiveSparseBitMatrix naiveSparseBitMatrix = NaiveSparseBitMatrix.createRandom(rows, columns, weight, SECURE_RANDOM);
            DenseBitMatrix denseBitMatrix = naiveSparseBitMatrix.toDense();
            Assert.assertArrayEquals(naiveSparseBitMatrix.lmul(v), denseBitMatrix.leftMultiply(v));
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
            boolean[] v = BinaryUtils.randomBinary(rows, SECURE_RANDOM);
            boolean[] t0 = BinaryUtils.randomBinary(columns, SECURE_RANDOM);
            boolean[] t1 = BinaryUtils.clone(t0);
            NaiveSparseBitMatrix naiveSparseBitMatrix = NaiveSparseBitMatrix.createRandom(rows, columns, weight, SECURE_RANDOM);
            DenseBitMatrix denseBitMatrix = naiveSparseBitMatrix.toDense();
            naiveSparseBitMatrix.lmulAddi(v, t0);
            denseBitMatrix.leftMultiplyXori(v, t1);
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
            byte[][] v = BytesUtils.randomByteArrayVector(rows, 16, SECURE_RANDOM);
            NaiveSparseBitMatrix naiveSparseBitMatrix = NaiveSparseBitMatrix.createRandom(rows, columns, weight, SECURE_RANDOM);
            DenseBitMatrix denseBitMatrix = naiveSparseBitMatrix.toDense();
            Assert.assertArrayEquals(naiveSparseBitMatrix.lExtMul(v), denseBitMatrix.leftGf2lMultiply(v));
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
            byte[][] v = BytesUtils.randomByteArrayVector(rows, 16, SECURE_RANDOM);
            byte[][] t0 = BytesUtils.randomByteArrayVector(columns, 16, SECURE_RANDOM);
            byte[][] t1 = Arrays.stream(t0).map(BytesUtils::clone).toArray(byte[][]::new);
            NaiveSparseBitMatrix naiveSparseBitMatrix = NaiveSparseBitMatrix.createRandom(rows, columns, weight, SECURE_RANDOM);
            DenseBitMatrix denseBitMatrix = naiveSparseBitMatrix.transposeDense().transpose(EnvType.STANDARD_JDK, false);
            naiveSparseBitMatrix.lExtMulAddi(v, t0);
            denseBitMatrix.leftGf2lMultiplyXori(v, t1);
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
        NaiveSparseBitMatrix whole = NaiveSparseBitMatrix.createRandom(rows, columns, weight, SECURE_RANDOM);

        int splitIndex = rows / 2;
        NaiveSparseBitMatrix subA = whole.subMatrix(0, columns, 0, splitIndex);
        NaiveSparseBitMatrix subB = whole.subMatrix(0, columns, splitIndex, rows);

        boolean[] v = BinaryUtils.randomBinary(rows, SECURE_RANDOM);

        boolean[] subV0 = Arrays.copyOfRange(v, 0, splitIndex);
        boolean[] subV1 = Arrays.copyOfRange(v, splitIndex, rows);
        boolean[] output0 = subA.lmul(subV0);
        boolean[] output1 = subB.lmul(subV1);
        for (int i = 0; i < output0.length; i++) {
            output0[i] ^= output1[i];
        }
        Assert.assertArrayEquals(whole.lmul(v), output0);
    }

}
