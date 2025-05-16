package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32WprfMatrixFactory.F32WprfMatrixType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * F32 weak PRF matrix test.
 *
 * @author Weiran Liu
 * @date 2024/10/16
 */
@RunWith(Parameterized.class)
public class F32WprfMatrixTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{F32WprfMatrixType.NAIVE});
        configurations.add(new Object[]{F32WprfMatrixType.BYTE});
        configurations.add(new Object[]{F32WprfMatrixType.LONG});

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
    private final F32WprfMatrixType type;

    public F32WprfMatrixTest(F32WprfMatrixType type) {
        secureRandom = new SecureRandom();
        z3Field = new Z3ByteField();
        this.type = type;
    }

    @Test
    public void testType() {
        F32WprfMatrix matrix = F32WprfMatrixFactory.createRandom(z3Field, secureRandom, type);
        Assert.assertEquals(type, matrix.getType());
    }

    @Test
    public void testRandom() {
        F32WprfMatrix matrix1, matrix2;
        // create without seed
        matrix1 = F32WprfMatrixFactory.createRandom(z3Field, secureRandom, type);
        matrix2 = F32WprfMatrixFactory.createRandom(z3Field, secureRandom, type);
        Assert.assertNotEquals(matrix1, matrix2);
        // create with same seed
        matrix1 = F32WprfMatrixFactory.createRandom(z3Field, BlockUtils.zeroBlock(), type);
        matrix2 = F32WprfMatrixFactory.createRandom(z3Field, BlockUtils.zeroBlock(), type);
        Assert.assertEquals(matrix1, matrix2);
    }

    @Test
    public void testLeftMul() {
        byte[][] elements = new byte[F32WprfMatrix.ROWS][F32WprfMatrix.COLUMNS];
        for (int i = 0; i < F32WprfMatrix.ROWS; i++) {
            for (int j = 0; j < F32WprfMatrix.COLUMNS; j++) {
                elements[i][j] = z3Field.createRandom(secureRandom);
            }
        }
        F32WprfMatrix matrix = F32WprfMatrixFactory.create(z3Field, elements, type);
        // generate a random input
        byte[] input = new byte[F32WprfMatrix.ROWS];
        for (int i = 0; i < F32WprfMatrix.ROWS; i++) {
            input[i] = z3Field.createRandom(secureRandom);
        }
        // manually computation
        byte[] expectOutput = new byte[F32WprfMatrix.COLUMNS];
        for (int j = 0; j < F32WprfMatrix.COLUMNS; j++) {
            for (int i = 0; i < F32WprfMatrix.ROWS; i++) {
                expectOutput[j] = z3Field.add(expectOutput[j], z3Field.mul(input[i], elements[i][j]));
            }
        }
        // matrix computation
        byte[] actualOutput = matrix.leftMul(input);
        Assert.assertArrayEquals(expectOutput, actualOutput);
    }
}
