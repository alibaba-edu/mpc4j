package edu.alibaba.mpc4j.common.tool.bristol;

import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrixFactory.DenseBitMatrixType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * Block Boolean Matrix Multiplication lookup table test.
 *
 * @author Weiran Liu
 * @date 2025/4/7
 */
public class BlockBmmLookupCodeTest {
    /**
     * round num
     */
    private static final int ROUND = 100;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public BlockBmmLookupCodeTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testConstantMatrix() {
        byte[] zero = new byte[BlockBmmLookupCode.BLOCK_BYTE_SIZE];
        byte[] one = BytesUtils.allOneByteArray(BlockBmmLookupCode.BLOCK_BIT_SIZE);
        byte[][] byteBitMatrix = new byte[BlockBmmLookupCode.BLOCK_BIT_SIZE][BlockBmmLookupCode.BLOCK_BYTE_SIZE];
        BlockBmmLookupCode lookupTable;

        // [0, 0, 0, 0]^T
        byteBitMatrix[0] = zero;
        byteBitMatrix[1] = zero;
        byteBitMatrix[2] = zero;
        byteBitMatrix[3] = zero;
        lookupTable = new BlockBmmLookupCode(byteBitMatrix);
        for (int j = 0; j < BlockBmmLookupCode.BLOCK_BIT_SIZE; j++) {
            Assert.assertEquals(0, lookupTable.getCode(0, j));
        }
        // [1, 0, 0, 0]^T
        byteBitMatrix[0] = one;
        byteBitMatrix[1] = zero;
        byteBitMatrix[2] = zero;
        byteBitMatrix[3] = zero;
        lookupTable = new BlockBmmLookupCode(byteBitMatrix);
        for (int j = 0; j < BlockBmmLookupCode.BLOCK_BIT_SIZE; j++) {
            Assert.assertEquals(1, lookupTable.getCode(0, j));
        }
        // [0, 1, 0, 0]^T
        byteBitMatrix = new byte[BlockBmmLookupCode.BLOCK_BIT_SIZE][BlockBmmLookupCode.BLOCK_BYTE_SIZE];
        byteBitMatrix[0] = zero;
        byteBitMatrix[1] = one;
        byteBitMatrix[2] = zero;
        byteBitMatrix[3] = zero;
        lookupTable = new BlockBmmLookupCode(byteBitMatrix);
        for (int j = 0; j < BlockBmmLookupCode.BLOCK_BIT_SIZE; j++) {
            Assert.assertEquals(3, lookupTable.getCode(0, j));
        }

        // [1, 1, 0, 0]^T
        byteBitMatrix = new byte[BlockBmmLookupCode.BLOCK_BIT_SIZE][BlockBmmLookupCode.BLOCK_BYTE_SIZE];
        byteBitMatrix[0] = one;
        byteBitMatrix[1] = one;
        byteBitMatrix[2] = zero;
        byteBitMatrix[3] = zero;
        lookupTable = new BlockBmmLookupCode(byteBitMatrix);
        for (int j = 0; j < BlockBmmLookupCode.BLOCK_BIT_SIZE; j++) {
            Assert.assertEquals(2, lookupTable.getCode(0, j));
        }

        // [0, 0, 1, 0]^T
        byteBitMatrix = new byte[BlockBmmLookupCode.BLOCK_BIT_SIZE][BlockBmmLookupCode.BLOCK_BYTE_SIZE];
        byteBitMatrix[0] = zero;
        byteBitMatrix[1] = zero;
        byteBitMatrix[2] = one;
        byteBitMatrix[3] = zero;
        lookupTable = new BlockBmmLookupCode(byteBitMatrix);
        for (int j = 0; j < BlockBmmLookupCode.BLOCK_BIT_SIZE; j++) {
            Assert.assertEquals(7, lookupTable.getCode(0, j));
        }
    }

    @Test
    public void testConstantInput() {
        for (int round = 0; round < ROUND; round++) {
            byte[][] byteBitMatrix = BlockUtils.randomBlocks(BlockBmmLookupCode.BLOCK_BIT_SIZE, secureRandom);
            BlockBmmLookupCode lookupTable = new BlockBmmLookupCode(byteBitMatrix);
            for (int i = 0; i < BlockBmmLookupCode.BLOCK_BIT_SIZE; i++) {
                byte[] v = new byte[BlockBmmLookupCode.BLOCK_BYTE_SIZE];
                BinaryUtils.setBoolean(v, i, true);
                byte[] expect = byteBitMatrix[i];
                byte[] actual = lookupTable.leftMultiply(v);
                Assert.assertArrayEquals(expect, actual);
            }
        }
    }

    @Test
    public void testRandom() {
        for (int round = 0; round < ROUND; round++) {
            byte[][] byteBitMatrix = BlockUtils.randomBlocks(BlockBmmLookupCode.BLOCK_BIT_SIZE, secureRandom);
            DenseBitMatrix denseBitMatrix = DenseBitMatrixFactory.createFromDense(
                DenseBitMatrixType.BYTE_MATRIX, BlockBmmLookupCode.BLOCK_BIT_SIZE, byteBitMatrix
            );
            BlockBmmLookupCode lookupTable = new BlockBmmLookupCode(byteBitMatrix);
            for (int i = 0; i < BlockBmmLookupCode.BLOCK_BIT_SIZE; i++) {
                byte[] v = BlockUtils.randomBlock(secureRandom);
                byte[] expect = denseBitMatrix.leftMultiply(v);
                byte[] actual = lookupTable.leftMultiply(v);
                Assert.assertArrayEquals(expect, actual);
            }
        }
    }
}
