package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * lower triangular sparse bit matrix test.
 *
 * @author Weiran Liu
 * @date 2023/6/27
 */
public class TriSparseBitMatrixTest {
    /**
     * sizes
     */
    private static final int[] SIZES = new int[]{40, 100, 200, 400};
    /**
     * weights
     */
    private static final int[] MAX_WEIGHTS = new int[]{5, 8, 16};
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * round
     */
    private static final int ROUND = 40;

    @Test
    public void testCreateRandomLowerTriangular() {
        LowerTriSquareSparseBitMatrix sparseBitMatrix;
        // create size = 1
        sparseBitMatrix = LowerTriSquareSparseBitMatrix.createRandom(1, 1, SECURE_RANDOM);
        assertLowerTriangular(1, sparseBitMatrix);
        assertMaxWeight(1, 1, sparseBitMatrix);
        // create sizes
        for (int size : SIZES) {
            for (int maxWeight : MAX_WEIGHTS) {
                for (int round = 0; round < ROUND; round++) {
                    sparseBitMatrix = LowerTriSquareSparseBitMatrix.createRandom(size, maxWeight, SECURE_RANDOM);
                    assertLowerTriangular(size, sparseBitMatrix);
                    assertMaxWeight(size, maxWeight, sparseBitMatrix);
                }
            }
        }
    }

    @Test
    public void testCreateRandomUpperTriangular() {
        UpperTriSquareSparseBitMatrix sparseBitMatrix;
        // create size = 1
        sparseBitMatrix = UpperTriSquareSparseBitMatrix.createRandom(1, 1, SECURE_RANDOM);
        assertUpperTriangular(1, sparseBitMatrix);
        assertMaxWeight(1, 1, sparseBitMatrix);
        // create sizes
        for (int size : SIZES) {
            for (int maxWeight : MAX_WEIGHTS) {
                for (int round = 0; round < ROUND; round++) {
                    sparseBitMatrix = UpperTriSquareSparseBitMatrix.createRandom(size, maxWeight, SECURE_RANDOM);
                    assertUpperTriangular(size, sparseBitMatrix);
                    assertMaxWeight(size, maxWeight, sparseBitMatrix);
                }
            }
        }
    }

    private void assertMaxWeight(int size, int maxWeight, TriSquareSparseBitMatrix sparseBitMatrix) {
        // verify max weight
        for (int iColumn = 0; iColumn < size; iColumn++) {
            SparseBitVector columnVector = sparseBitMatrix.getColumn(iColumn);
            Assert.assertTrue(maxWeight >= columnVector.getSize());
            Assert.assertTrue(columnVector.getSize() >= 1);
        }
    }

    @Test
    public void testLowerTriangularTranspose() {
        LowerTriSquareSparseBitMatrix sparseBitMatrix;
        // create size = 1
        sparseBitMatrix = LowerTriSquareSparseBitMatrix.createRandom(1, 1, SECURE_RANDOM);
        assertLowerTriangularTranspose(1, sparseBitMatrix);
        for (int size : SIZES) {
            for (int maxWeight : MAX_WEIGHTS) {
                for (int round = 0; round < ROUND; round++) {
                    sparseBitMatrix = LowerTriSquareSparseBitMatrix.createRandom(size, maxWeight, SECURE_RANDOM);
                    assertLowerTriangularTranspose(size, sparseBitMatrix);
                }
            }
        }
    }

    private void assertLowerTriangularTranspose(int size, LowerTriSquareSparseBitMatrix sparseBitMatrix) {
        // (M^T)^T = M
        UpperTriSquareSparseBitMatrix transSparseBitMatrix = sparseBitMatrix.transpose();
        assertUpperTriangular(size, transSparseBitMatrix);
        LowerTriSquareSparseBitMatrix transTransSparseBitMatrix = transSparseBitMatrix.transpose();
        assertLowerTriangular(size, transTransSparseBitMatrix);
        Assert.assertEquals(sparseBitMatrix, transTransSparseBitMatrix);
    }

    @Test
    public void testUpperTriangularTranspose() {
        UpperTriSquareSparseBitMatrix sparseBitMatrix;
        // create size = 1
        sparseBitMatrix = UpperTriSquareSparseBitMatrix.createRandom(1, 1, SECURE_RANDOM);
        assertUpperTriangularTranspose(1, sparseBitMatrix);
        for (int size : SIZES) {
            for (int maxWeight : MAX_WEIGHTS) {
                for (int round = 0; round < ROUND; round++) {
                    sparseBitMatrix = UpperTriSquareSparseBitMatrix.createRandom(size, maxWeight, SECURE_RANDOM);
                    assertUpperTriangularTranspose(size, sparseBitMatrix);
                }
            }
        }
    }

    private void assertUpperTriangularTranspose(int size, UpperTriSquareSparseBitMatrix sparseBitMatrix) {
        // (M^T)^T = M
        LowerTriSquareSparseBitMatrix transSparseBitMatrix = sparseBitMatrix.transpose();
        assertLowerTriangularTranspose(size, transSparseBitMatrix);
        UpperTriSquareSparseBitMatrix transTransSparseBitMatrix = transSparseBitMatrix.transpose();
        assertUpperTriangular(size, transTransSparseBitMatrix);
        Assert.assertEquals(sparseBitMatrix, transTransSparseBitMatrix);
    }

    private void assertLowerTriangular(int size, LowerTriSquareSparseBitMatrix sparseBitMatrix) {
        Assert.assertEquals(size, sparseBitMatrix.getSize());
        Assert.assertEquals(size, sparseBitMatrix.getRows());
        Assert.assertEquals(size, sparseBitMatrix.getColumns());
        for (int iRow = 0; iRow < size; iRow++) {
            for (int iColumn = iRow; iColumn < size; iColumn++) {
                if (iColumn == iRow) {
                    // diagonal
                    Assert.assertTrue(sparseBitMatrix.get(iRow, iColumn));
                } else {
                    // entries above diagonal
                    Assert.assertFalse(sparseBitMatrix.get(iRow, iColumn));
                }
            }
        }
    }

    private void assertUpperTriangular(int size, UpperTriSquareSparseBitMatrix sparseBitMatrix) {
        Assert.assertEquals(size, sparseBitMatrix.getSize());
        Assert.assertEquals(size, sparseBitMatrix.getRows());
        Assert.assertEquals(size, sparseBitMatrix.getColumns());
        for (int iRow = 0; iRow < size; iRow++) {
            for (int iColumn = 0; iColumn <= iRow; iColumn++) {
                if (iColumn == iRow) {
                    // diagonal
                    Assert.assertTrue(sparseBitMatrix.get(iRow, iColumn));
                } else {
                    // entries above diagonal
                    Assert.assertFalse(sparseBitMatrix.get(iRow, iColumn));
                }
            }
        }
    }

    @Test
    public void testLowerTriangularMultiply() {
        LowerTriSquareSparseBitMatrix sparseBitMatrix;
        // create size = 1
        sparseBitMatrix = LowerTriSquareSparseBitMatrix.createRandom(1, 1, SECURE_RANDOM);
        assertMultiply(1, sparseBitMatrix);
        for (int size : SIZES) {
            for (int maxWeight : MAX_WEIGHTS) {
                for (int round = 0; round < ROUND; round++) {
                    sparseBitMatrix = LowerTriSquareSparseBitMatrix.createRandom(size, maxWeight, SECURE_RANDOM);
                    assertMultiply(size, sparseBitMatrix);
                }
            }
        }
    }

    @Test
    public void testUpperTriangularMultiply() {
        UpperTriSquareSparseBitMatrix sparseBitMatrix;
        // create size = 1
        sparseBitMatrix = UpperTriSquareSparseBitMatrix.createRandom(1, 1, SECURE_RANDOM);
        assertMultiply(1, sparseBitMatrix);
        for (int size : SIZES) {
            for (int maxWeight : MAX_WEIGHTS) {
                for (int round = 0; round < ROUND; round++) {
                    sparseBitMatrix = UpperTriSquareSparseBitMatrix.createRandom(size, maxWeight, SECURE_RANDOM);
                    assertMultiply(size, sparseBitMatrix);
                }
            }
        }
    }

    private void assertMultiply(int size, TriSquareSparseBitMatrix sparseBitMatrix) {
        for (int round = 0; round < ROUND; round++) {
            // x · M · M^{-1} = x
            boolean[] x0 = BinaryUtils.randomBinary(size, SECURE_RANDOM);
            boolean[] y = sparseBitMatrix.lmul(x0);
            boolean[] x1 = sparseBitMatrix.invLmul(y);
            Assert.assertArrayEquals(x0, x1);
        }
    }

    @Test
    public void testLowerTriangularGf2lMultiply() {
        LowerTriSquareSparseBitMatrix sparseBitMatrix;
        // create size = 1
        sparseBitMatrix = LowerTriSquareSparseBitMatrix.createRandom(1, 1, SECURE_RANDOM);
        assertGf2lMultiply(1, sparseBitMatrix);
        for (int size : SIZES) {
            for (int maxWeight : MAX_WEIGHTS) {
                for (int round = 0; round < ROUND; round++) {
                    sparseBitMatrix = LowerTriSquareSparseBitMatrix.createRandom(size, maxWeight, SECURE_RANDOM);
                    assertGf2lMultiply(size, sparseBitMatrix);
                }
            }
        }
    }

    @Test
    public void testUpperTriangularGf2lMultiply() {
        UpperTriSquareSparseBitMatrix sparseBitMatrix;
        // create size = 1
        sparseBitMatrix = UpperTriSquareSparseBitMatrix.createRandom(1, 1, SECURE_RANDOM);
        assertGf2lMultiply(1, sparseBitMatrix);
        for (int size : SIZES) {
            for (int maxWeight : MAX_WEIGHTS) {
                for (int round = 0; round < ROUND; round++) {
                    sparseBitMatrix = UpperTriSquareSparseBitMatrix.createRandom(size, maxWeight, SECURE_RANDOM);
                    assertGf2lMultiply(size, sparseBitMatrix);
                }
            }
        }
    }

    private void assertGf2lMultiply(int size, TriSquareSparseBitMatrix sparseBitMatrix) {
        int byteSize = CommonUtils.getByteLength(size);
        for (int round = 0; round < ROUND; round++) {
            // x · M · M^{-1} = x
            byte[][] x0 = BytesUtils.randomByteArrayVector(size, byteSize, SECURE_RANDOM);
            byte[][] y = sparseBitMatrix.lExtMul(x0);
            byte[][] x1 = sparseBitMatrix.invLextMul(y);
            Assert.assertArrayEquals(x0, x1);
        }
    }
}
