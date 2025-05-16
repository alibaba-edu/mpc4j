package edu.alibaba.mpc4j.common.structure.matrix;

import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * int matrix test.
 *
 * @author Weiran Liu
 * @date 2024/7/5
 */
public class IntMatrixTest {
    /**
     * rows
     */
    private final int rows;
    /**
     * columns
     */
    private final int columns;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public IntMatrixTest() {
        rows = 63;
        columns = 65;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testCreateRandomWithSeed() {
        // create with the same seed
        byte[] seed = BlockUtils.randomBlock(secureRandom);
        IntMatrix matrix1 = IntMatrix.createRandom(rows, columns, seed);
        IntMatrix matrix2 = IntMatrix.createRandom(rows, columns, seed);
        Assert.assertEquals(matrix1, matrix2);
        // create with different seeds
        for (int r = 0; r < 10; r++) {
            byte[] seed1 = BlockUtils.randomBlock(secureRandom);
            byte[] seed2 = BlockUtils.randomBlock(secureRandom);
            matrix1 = IntMatrix.createRandom(rows, columns, seed1);
            matrix2 = IntMatrix.createRandom(rows, columns, seed2);
            Assert.assertNotEquals(matrix1, matrix2);
        }
    }

    @Test
    public void testConcat() {
        IntMatrix matrix = IntMatrix.createRandom(rows, columns, secureRandom);
        // concat with different columns
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            IntVector that = IntVector.createRandom(columns - 1, secureRandom);
            matrix.concat(that);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            IntVector that = IntVector.createRandom(columns + 1, secureRandom);
            matrix.concat(that);
        });
        IntVector that = IntVector.createRandom(columns, secureRandom);
        IntMatrix appendMatrix = matrix.concat(that);
        Assert.assertEquals(rows + 1, appendMatrix.getRows());
        Assert.assertEquals(columns, appendMatrix.getColumns());
        for (int i = 0; i < appendMatrix.getRows(); i++) {
            if (i < rows) {
                Assert.assertEquals(matrix.getRow(i), appendMatrix.getRow(i));
            } else {
                Assert.assertEquals(that, appendMatrix.getRow(i));
            }
        }
    }

    @Test
    public void testTranspose() {
        IntMatrix matrix = IntMatrix.createRandom(rows, columns, secureRandom);
        IntMatrix tMatrix = matrix.transpose();
        Assert.assertEquals(columns, tMatrix.getRows());
        Assert.assertEquals(rows, tMatrix.getColumns());
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                Assert.assertEquals(matrix.get(i, j), tMatrix.get(j, i));
            }
        }
        IntMatrix ttMatrix = tMatrix.transpose();
        Assert.assertEquals(matrix, ttMatrix);
    }

    @Test
    public void testDecompose() {
        IntMatrix matrix = IntMatrix.createRandom(rows, columns, secureRandom);
        // p > 1
        Assert.assertThrows(IllegalArgumentException.class, () -> IntMatrix.decompose(matrix, 0));
        Assert.assertThrows(IllegalArgumentException.class, () -> IntMatrix.decompose(matrix, 1));

        for (int i = 0; i < 10; i++) {
            // p ∈ [2, 12)
            int p = secureRandom.nextInt(10) + 2;
            // decompose
            IntMatrix[] decomposedMatrices = IntMatrix.decompose(matrix, p);
            // verify one result
            IntVector vector = IntVector.createZeros(columns);
            for (IntMatrix decomposedMatrix : decomposedMatrices) {
                vector.muli(p);
                vector.addi(decomposedMatrix.getRow(0));
            }
            Assert.assertEquals(matrix.getRow(0), vector);
            // compose
            IntMatrix composedMatrix = IntMatrix.compose(decomposedMatrices, p);
            Assert.assertEquals(matrix, composedMatrix);
        }
    }

    @Test
    public void testDecomposeByteVector() {
        IntMatrix matrix = IntMatrix.createRandom(rows, columns, secureRandom);
        // decompose
        IntMatrix[] decomposedMatrices = IntMatrix.decomposeToByteVector(matrix);
        // verify one result
        IntVector vector = IntVector.createZeros(columns);
        for (IntMatrix decomposedMatrix : decomposedMatrices) {
            vector.shiftLefti(Byte.SIZE);
            vector.addi(decomposedMatrix.getRow(0));
        }
        Assert.assertEquals(matrix.getRow(0), vector);
        // compose
        IntMatrix composedMatrix = IntMatrix.composeByteVector(decomposedMatrices);
        Assert.assertEquals(matrix, composedMatrix);
    }
}
