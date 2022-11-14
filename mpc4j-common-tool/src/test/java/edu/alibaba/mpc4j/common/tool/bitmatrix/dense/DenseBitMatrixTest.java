package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * 稠密布尔矩阵测试。
 *
 * @author Weiran Liu
 * @date 2022/8/2
 */
public class DenseBitMatrixTest {
    /**
     * 测试维度
     */
    private static final int[] SIZES = new int[]{1, 7, 8, 9, 127, 128, 129};
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 随机测试轮数
     */
    private static final int ROUND = 40;

    @Test
    public void testConstantAdd() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                // 0 + 0 = 0
                DenseBitMatrix zero = DenseBitMatrixTestUtils.createAllZero(rows, columns);
                Assert.assertEquals(zero, zero.add(zero));
                DenseBitMatrix inner = DenseBitMatrixTestUtils.createAllZero(rows, columns);
                inner.addi(zero);
                Assert.assertEquals(zero, inner);
                // 0 + 1 = 1
                DenseBitMatrix one = DenseBitMatrixTestUtils.createAllOne(rows, columns);
                Assert.assertEquals(one, one.add(zero));
                Assert.assertEquals(one, zero.add(one));
                inner = DenseBitMatrixTestUtils.createAllZero(rows, columns);
                inner.addi(one);
                Assert.assertEquals(one, inner);
                inner = DenseBitMatrixTestUtils.createAllOne(rows, columns);
                inner.addi(zero);
                Assert.assertEquals(one, inner);
                // 1 + 1 = 0
                Assert.assertEquals(zero, one.add(one));
                inner = DenseBitMatrixTestUtils.createAllOne(rows, columns);
                inner.addi(one);
                Assert.assertEquals(zero, inner);
            }
        }
    }

    @Test
    public void testRandomAdd() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                testRandomAdd(rows, columns);
            }
        }
    }

    private void testRandomAdd(int rows, int columns) {
        DenseBitMatrix zero = DenseBitMatrixTestUtils.createAllZero(rows, columns);
        DenseBitMatrix inner;
        // random + random = 0
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix random = DenseBitMatrixTestUtils.createRandom(rows, columns, SECURE_RANDOM);
            Assert.assertEquals(zero, random.add(random));
            inner = DenseBitMatrixTestUtils.createRandom(rows, columns, SECURE_RANDOM);
            inner.addi(inner);
            Assert.assertEquals(zero, inner);
        }
        // random + 0 = random
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix random = DenseBitMatrixTestUtils.createRandom(rows, columns, SECURE_RANDOM);
            Assert.assertEquals(random, random.add(zero));
        }
    }

    @Test
    public void testRandomMultiply() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                testRandomMultiply(rows, columns);
            }
        }
    }

    private void testRandomMultiply(int rows, int columns) {
        for (int rightColumns : SIZES) {
            for (int round = 0; round < ROUND; round++) {
                DenseBitMatrix a = DenseBitMatrixTestUtils.createRandom(rows, columns, SECURE_RANDOM);
                // 右侧矩阵的行数必须等于左侧矩阵的列数
                DenseBitMatrix b = DenseBitMatrixTestUtils.createRandom(columns, rightColumns, SECURE_RANDOM);
                // 测试方法：(A * B)^T = B^T * A^T
                DenseBitMatrix mulTrans = a.multiply(b).transpose(EnvType.STANDARD, false);
                DenseBitMatrix transMul = b.transpose(EnvType.STANDARD, false)
                    .multiply(a.transpose(EnvType.STANDARD, false));
                Assert.assertEquals(mulTrans, transMul);
            }
        }
    }

    @Test
    public void testLmul() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                testLmul(rows, columns);
            }
        }
    }

    private void testLmul(int rows, int columns) {
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixTestUtils.createRandom(rows, rows, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixTestUtils.createRandom(rows, columns, SECURE_RANDOM);
            byte[][] expectArray = a.multiply(b).toByteArrays();
            // 将a矩阵分别转换成byte[]分别与b矩阵左乘
            byte[][] byteVectorActualArray = IntStream.range(0, rows)
                .mapToObj(rowIndex -> {
                    byte[] v = a.getRow(rowIndex);
                    return b.lmul(v);
                })
                .toArray(byte[][]::new);
            Assert.assertArrayEquals(expectArray, byteVectorActualArray);
            // 将a矩阵转换为布尔矩阵，分别与b矩阵相乘
            byte[][] binaryVectorActualArray = IntStream.range(0, rows)
                .mapToObj(rowIndex -> {
                    boolean[] v = BinaryUtils.byteArrayToBinary(a.getRow(rowIndex), rows);
                    return b.lmul(v);
                })
                .map(BinaryUtils::binaryToRoundByteArray)
                .toArray(byte[][]::new);
            Assert.assertArrayEquals(expectArray, binaryVectorActualArray);
        }
    }

    @Test
    public void testLmulAddi() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                testLmulAddi(rows, columns);
            }
        }
    }

    private void testLmulAddi(int rows, int columns) {
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixTestUtils.createRandom(rows, rows, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixTestUtils.createRandom(rows, columns, SECURE_RANDOM);
            DenseBitMatrix c = DenseBitMatrixTestUtils.createRandom(rows, columns, SECURE_RANDOM);
            byte[][] expectArray = a.multiply(b).add(c).toByteArrays();
            // 将a矩阵分别转换成byte[]分别与b矩阵左乘
            byte[][] byteVectorActualArray = IntStream.range(0, rows)
                .mapToObj(rowIndex -> {
                    byte[] t = BytesUtils.clone(c.getRow(rowIndex));
                    b.lmulAddi(a.getRow(rowIndex), t);
                    return t;
                })
                .toArray(byte[][]::new);
            Assert.assertArrayEquals(expectArray, byteVectorActualArray);
        }
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixTestUtils.createRandom(rows, rows, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixTestUtils.createRandom(rows, columns, SECURE_RANDOM);
            DenseBitMatrix c = DenseBitMatrixTestUtils.createRandom(rows, columns, SECURE_RANDOM);
            byte[][] expectArray = a.multiply(b).add(c).toByteArrays();
            // 将a矩阵分别转换成boolean[]分别与b矩阵左乘
            byte[][] byteVectorActualArray = IntStream.range(0, rows)
                .mapToObj(rowIndex -> {
                    boolean[] v = BinaryUtils.byteArrayToBinary(a.getRow(rowIndex), rows);
                    boolean[] t = BinaryUtils.byteArrayToBinary(c.getRow(rowIndex), columns);
                    b.lmulAddi(v, t);
                    return BinaryUtils.binaryToRoundByteArray(t);
                })
                .toArray(byte[][]::new);
            Assert.assertArrayEquals(expectArray, byteVectorActualArray);
        }
    }

    @Test
    public void testTranspose() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                testTranspose(rows, columns);
            }
        }
    }

    private void testTranspose(int rows, int columns) {
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix origin = DenseBitMatrixTestUtils.createRandom(rows, columns, SECURE_RANDOM);
            DenseBitMatrix transpose = origin.transpose(EnvType.STANDARD, false);
            DenseBitMatrix recover = transpose.transpose(EnvType.STANDARD, false);
            Assert.assertEquals(origin, recover);
        }
    }

    @Test
    public void testLextMul() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                testLextMul(rows, columns);
            }
        }
    }

    private void testLextMul(int rows, int columns) {
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixTestUtils.createRandom(rows, rows, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixTestUtils.createRandom(rows, columns, SECURE_RANDOM);
            // 测试方法： (A^T*B)^T = (A.toArrays())*B
            DenseBitMatrix aTranspose = a.transpose(EnvType.STANDARD_JDK, false);
            byte[][] expectArray = aTranspose.multiply(b).transpose(EnvType.STANDARD_JDK, false).toByteArrays();
            byte[][] byteVectorActualArray = b.lExtMul(a.toByteArrays());
            Assert.assertArrayEquals(expectArray, byteVectorActualArray);
        }
    }

    @Test
    public void testLextMulAddi(){
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                testLextMulAddi(rows, columns);
            }
        }
    }

    private void testLextMulAddi(int rows, int columns) {
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixTestUtils.createRandom(rows, rows, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixTestUtils.createRandom(rows, columns, SECURE_RANDOM);
            DenseBitMatrix c = DenseBitMatrixTestUtils.createRandom(rows, columns, SECURE_RANDOM);
            // 测试方法： ((A^T*B) + C)^T = (A.toArrays())*B + C^T.toArray()
            DenseBitMatrix aTranspose = a.transpose(EnvType.STANDARD_JDK, false);
            byte[][] expectArray = aTranspose.multiply(b).add(c).transpose(EnvType.STANDARD_JDK, false).toByteArrays();
            byte[][] byteVectorActualArray = c.transpose(EnvType.STANDARD_JDK, false).toByteArrays();
            b.lExtMulAddi(a.toByteArrays(), byteVectorActualArray);
            Assert.assertArrayEquals(expectArray, byteVectorActualArray);
        }
    }
}
