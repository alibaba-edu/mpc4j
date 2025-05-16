package edu.alibaba.mpc4j.common.tool.bristol;

import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrixFactory.DenseBitMatrixType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.security.SecureRandom;

/**
 * Bristol Fashion BMM generator test.
 *
 * @author Weiran Liu
 * @date 2025/4/9
 */
public class BristolFashionBmmGeneratorTest {
    /**
     * round num
     */
    private static final int ROUND = 100;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public BristolFashionBmmGeneratorTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testConstantInput() throws IOException {
        // generate the circuit
        BristolFashionBmmGenerator generator = new BristolFashionBmmGenerator();
        byte[][] matrix = BlockUtils.randomBlocks(BlockBmmLookupCode.BLOCK_BIT_SIZE, secureRandom);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generate(matrix, outputStream);
        byte[] circuit = outputStream.toByteArray();
        outputStream.close();

        // verify output
        ByteArrayInputStream inputStream = new ByteArrayInputStream(circuit);
        BristolFashionEvaluator evaluator = new BristolFashionEvaluator(inputStream);
        inputStream.close();
        DenseBitMatrix denseBitMatrix = DenseBitMatrixFactory.createFromDense(
            DenseBitMatrixType.BYTE_MATRIX, BlockBmmLookupCode.BLOCK_BIT_SIZE, matrix
        );
        for (int round = 0; round < ROUND; round++) {
            for (int i = 0; i < BlockBmmLookupCode.BLOCK_BIT_SIZE; i++) {
                byte[] v = new byte[BlockBmmLookupCode.BLOCK_BYTE_SIZE];
                BinaryUtils.setBoolean(v, i, true);
                byte[] expect = denseBitMatrix.leftMultiply(v);
                byte[] actual = BinaryUtils.binaryToByteArray(evaluator.evaluate(BinaryUtils.byteArrayToBinary(v)));
                Assert.assertArrayEquals(expect, actual);
            }
        }
    }

    @Test
    public void testRandom() throws IOException {
        BristolFashionBmmGenerator generator = new BristolFashionBmmGenerator();
        for (int round = 0; round < ROUND; round++) {
            byte[][] matrix = BlockUtils.randomBlocks(BlockBmmLookupCode.BLOCK_BIT_SIZE, secureRandom);
            DenseBitMatrix denseBitMatrix = DenseBitMatrixFactory.createFromDense(
                DenseBitMatrixType.BYTE_MATRIX, BlockBmmLookupCode.BLOCK_BIT_SIZE, matrix
            );
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            generator.generate(matrix, outputStream);
            byte[] circuit = outputStream.toByteArray();
            outputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(circuit);
            BristolFashionEvaluator evaluator = new BristolFashionEvaluator(inputStream);
            inputStream.close();
            for (int i = 0; i < BlockBmmLookupCode.BLOCK_BIT_SIZE; i++) {
                byte[] v = BlockUtils.randomBlock(secureRandom);
                byte[] expect = denseBitMatrix.leftMultiply(v);
                byte[] actual = BinaryUtils.binaryToByteArray(evaluator.evaluate(BinaryUtils.byteArrayToBinary(v)));
                Assert.assertArrayEquals(expect, actual);
            }
        }
    }
}
