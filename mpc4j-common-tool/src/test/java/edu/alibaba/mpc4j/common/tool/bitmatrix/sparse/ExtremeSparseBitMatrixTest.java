package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * extreme sparse bit matrix test.
 *
 * @author Weiran Liu
 * @date 2023/6/27
 */
public class ExtremeSparseBitMatrixTest {
    /**
     * sizes
     */
    private static final int[] SIZES = new int[]{40, 100, 200, 400};
    /**
     * weights
     */
    private static final int[] WEIGHTS = new int[]{5, 8, 16};
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Test
    public void testExtremeSparseMatrix() {
        for (int rows : SIZES) {
            for (int cols : SIZES) {
                for (int weight : WEIGHTS)
                    testExtremeSparseMatrix(rows, cols, weight);
            }
        }
    }

    private void testExtremeSparseMatrix(int rows, int columns, int weight) {
        NaiveSparseBitMatrix naiveSparseBitMatrix = NaiveSparseBitMatrix.createRandom(
            rows, columns, weight, SECURE_RANDOM
        );
        ExtremeSparseBitMatrix extremeSparseBitMatrix = naiveSparseBitMatrix.toExtremeSparseBitMatrix();
        boolean[] v = BinaryUtils.randomBinary(rows, SECURE_RANDOM);
        byte[][] elements = BytesUtils.randomByteArrayVector(rows, 16, SECURE_RANDOM);
        Assert.assertArrayEquals(naiveSparseBitMatrix.lmul(v), extremeSparseBitMatrix.lmul(v));
        Assert.assertArrayEquals(naiveSparseBitMatrix.lExtMul(elements), extremeSparseBitMatrix.lExtMul(elements));
    }
}
