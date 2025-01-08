package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3Utils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F23WprfMatrixFactory.F23WprfMatrixType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * F2 -> F3 weak PRF matrix test.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
@RunWith(Parameterized.class)
public class F23WprfMatrixTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{F23WprfMatrixType.NAIVE});
        configurations.add(new Object[]{F23WprfMatrixType.BYTE});
        configurations.add(new Object[]{F23WprfMatrixType.LONG});

        return configurations;
    }

    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * Z3-field
     */
    private final Z3ByteField z3Field;
    /**
     * matrix type
     */
    private final F23WprfMatrixType type;

    public F23WprfMatrixTest(F23WprfMatrixType type) {
        secureRandom = new SecureRandom();
        z3Field = new Z3ByteField();
        this.type = type;
    }

    @Test
    public void testType() {
        F23WprfMatrix matrix = F23WprfMatrixFactory.createRandom(z3Field, secureRandom, type);
        Assert.assertEquals(type, matrix.getType());
    }

    @Test
    public void testRandom() {
        F23WprfMatrix matrix1, matrix2;
        // create without seed
        matrix1 = F23WprfMatrixFactory.createRandom(z3Field, secureRandom, type);
        matrix2 = F23WprfMatrixFactory.createRandom(z3Field, secureRandom, type);
        Assert.assertNotEquals(matrix1, matrix2);
        // create with same seed
        matrix1 = F23WprfMatrixFactory.createRandom(z3Field, new byte[CommonConstants.BLOCK_BYTE_LENGTH], type);
        matrix2 = F23WprfMatrixFactory.createRandom(z3Field, new byte[CommonConstants.BLOCK_BYTE_LENGTH], type);
        Assert.assertEquals(matrix1, matrix2);
    }

    @Test
    public void testLeftBinaryMul() {
        byte[][] elements = new byte[F23WprfMatrix.ROWS][F23WprfMatrix.COLUMNS];
        for (int i = 0; i < F23WprfMatrix.ROWS; i++) {
            for (int j = 0; j < F23WprfMatrix.COLUMNS; j++) {
                elements[i][j] = z3Field.createRandom(secureRandom);
            }
        }
        F23WprfMatrix matrix = F23WprfMatrixFactory.create(z3Field, elements, type);
        // generate a random input
        byte[] input = new byte[F23WprfMatrix.ROW_BINARY_BYTES];
        secureRandom.nextBytes(input);
        // manually computation
        byte[] expectOutput = new byte[F23WprfMatrix.COLUMNS];
        for (int j = 0; j < F23WprfMatrix.COLUMNS; j++) {
            for (int i = 0; i < F23WprfMatrix.ROWS; i++) {
                boolean select = BinaryUtils.getBoolean(input, i);
                if (select) {
                    expectOutput[j] = z3Field.add(expectOutput[j], elements[i][j]);
                }
            }
        }
        // matrix computation
        byte[] actualOutput = matrix.leftBinaryMul(input);
        Assert.assertArrayEquals(expectOutput, actualOutput);
    }

    @Test
    public void testLeftMul() {
        byte[][] elements = new byte[F23WprfMatrix.ROWS][F23WprfMatrix.COLUMNS];
        for (int i = 0; i < F23WprfMatrix.ROWS; i++) {
            for (int j = 0; j < F23WprfMatrix.COLUMNS; j++) {
                elements[i][j] = z3Field.createRandom(secureRandom);
            }
        }
        F23WprfMatrix matrix = F23WprfMatrixFactory.create(z3Field, elements, type);
        // generate a random input
        byte[] input = new byte[F23WprfMatrix.ROWS];
        for (int i = 0; i < F23WprfMatrix.ROWS; i++) {
            input[i] = z3Field.createRandom(secureRandom);
        }
        // manually computation
        byte[] expectOutput = new byte[F23WprfMatrix.COLUMNS];
        for (int j = 0; j < F23WprfMatrix.COLUMNS; j++) {
            for (int i = 0; i < F23WprfMatrix.ROWS; i++) {
                expectOutput[j] = z3Field.add(expectOutput[j], z3Field.mul(input[i], elements[i][j]));
            }
        }
        // matrix computation
        byte[] actualOutput = matrix.leftMul(input);
        Assert.assertArrayEquals(expectOutput, actualOutput);
    }

    @Test
    public void testLeftCompressMul() {
        byte[][] elements = new byte[F23WprfMatrix.ROWS][F23WprfMatrix.COLUMNS];
        for (int i = 0; i < F23WprfMatrix.ROWS; i++) {
            for (int j = 0; j < F23WprfMatrix.COLUMNS; j++) {
                elements[i][j] = z3Field.createRandom(secureRandom);
            }
        }
        F23WprfMatrix matrix = F23WprfMatrixFactory.create(z3Field, elements, type);
        // generate a random input
        byte[] input = new byte[F23WprfMatrix.ROWS];
        for (int i = 0; i < F23WprfMatrix.ROWS; i++) {
            input[i] = z3Field.createRandom(secureRandom);
        }
        // manually computation
        byte[] expectOutput = new byte[F23WprfMatrix.COLUMNS];
        for (int j = 0; j < F23WprfMatrix.COLUMNS; j++) {
            for (int i = 0; i < F23WprfMatrix.ROWS; i++) {
                expectOutput[j] = z3Field.add(expectOutput[j], z3Field.mul(input[i], elements[i][j]));
            }
        }
        // byte multiplication
        byte[] compressByteInput = Z3Utils.compressToByteArray(input);
        byte[] actualOutput = matrix.leftCompressMul(compressByteInput);
        Assert.assertArrayEquals(expectOutput, actualOutput);
        // long multiplication
        long[] compressLongInput = Z3Utils.compressToLongArray(input);
        actualOutput = matrix.leftCompressMul(compressLongInput);
        Assert.assertArrayEquals(expectOutput, actualOutput);
    }
}
