package edu.alibaba.mpc4j.common.structure.matrix;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * Z3 Byte Matrix Test.
 *
 * @author Weiran Liu
 * @date 2024/5/22
 */
public class Z3ByteMatrixTest {
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * Z3-field
     */
    private final Z3ByteField z3Field;

    public Z3ByteMatrixTest() {
        secureRandom = new SecureRandom();
        z3Field = new Z3ByteField();
    }

    @Test
    public void testCreateRandom() {
        testCreateRandom(256, 128);
        testCreateRandom(128, 256);
    }

    private void testCreateRandom(int rows, int cols) {
        Z3ByteMatrix z3ByteMatrix1, z3ByteMatrix2;
        // create without seed
        z3ByteMatrix1 = Z3ByteMatrix.createRandom(z3Field, rows, cols, secureRandom);
        z3ByteMatrix2 = Z3ByteMatrix.createRandom(z3Field, rows, cols, secureRandom);
        Assert.assertNotEquals(z3ByteMatrix1, z3ByteMatrix2);
        // create with same seed
        z3ByteMatrix1 = Z3ByteMatrix.createRandom(z3Field, rows, cols, new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        z3ByteMatrix2 = Z3ByteMatrix.createRandom(z3Field, rows, cols, new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        Assert.assertEquals(z3ByteMatrix1, z3ByteMatrix2);
    }

    @Test
    public void testLeftMul() {
        testLeftMul(256, 128);
        testLeftMul(128, 256);
    }

    private void testLeftMul(int rows, int cols) {
        byte[][] elements = new byte[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                elements[i][j] = z3Field.createRandom(secureRandom);
            }
        }
        Z3ByteMatrix z3ByteMatrix = Z3ByteMatrix.create(z3Field, elements);
        // generate a random input
        byte[] input = new byte[rows];
        for (int i = 0; i < rows; i++) {
            input[i] = z3Field.createRandom(secureRandom);
        }
        // manually computation
        byte[] expectOutput = new byte[cols];
        for (int j = 0; j < cols; j++) {
            for (int i = 0; i < rows; i++) {
                expectOutput[j] = z3Field.add(expectOutput[j], z3Field.mul(input[i], elements[i][j]));
            }
        }
        // matrix computation
        byte[] actualOutput = z3ByteMatrix.leftMul(input);
        Assert.assertArrayEquals(expectOutput, actualOutput);
    }
}
